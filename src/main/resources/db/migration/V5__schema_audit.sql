-- =============================================================================
-- Schema ng_audit — Auditoria imutável de banco e logs de sistema
-- RNF-SEC.8 / RNF-PRIV.8 / LGPD art. 37 (registro de operações)
--
-- IMUTABILIDADE: triggers nos dois sentidos bloqueiam qualquer UPDATE ou DELETE
-- nas tabelas de auditoria diretamente. Apenas INSERT é permitido.
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS ng_audit;

-- ---------------------------------------------------------------------------
-- 1. AUDITORIA DE BANCO (ng_db_audit)
--    Registro imutável de cada INSERT/UPDATE/DELETE executado nas tabelas
--    críticas da aplicação. Alimentado por triggers individuais.
-- ---------------------------------------------------------------------------
CREATE TABLE ng_audit.ng_db_audit (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    occurred_at     TIMESTAMPTZ NOT NULL    DEFAULT NOW(),
    operation       VARCHAR(10) NOT NULL,          -- INSERT | UPDATE | DELETE
    schema_name     VARCHAR(63) NOT NULL,
    table_name      VARCHAR(63) NOT NULL,
    record_id       TEXT,                          -- valor da PK da linha afetada
    old_data        JSONB,                         -- dados antes (UPDATE/DELETE)
    new_data        JSONB,                         -- dados depois (INSERT/UPDATE)
    db_user         VARCHAR(128) NOT NULL DEFAULT current_user,
    app_user_id     UUID,                          -- ID do usuário JWT, se propagado
    client_addr     INET,
    tx_id           BIGINT      NOT NULL DEFAULT txid_current(),
    CONSTRAINT ng_db_audit_op_check CHECK (operation IN ('INSERT','UPDATE','DELETE'))
);

-- Índices de consulta forense
CREATE INDEX idx_db_audit_table_time   ON ng_audit.ng_db_audit (schema_name, table_name, occurred_at DESC);
CREATE INDEX idx_db_audit_app_user     ON ng_audit.ng_db_audit (app_user_id, occurred_at DESC);
CREATE INDEX idx_db_audit_record       ON ng_audit.ng_db_audit (table_name, record_id);
CREATE INDEX idx_db_audit_tx           ON ng_audit.ng_db_audit (tx_id);

-- ---------------------------------------------------------------------------
-- 2. LOGS DE SISTEMA (ng_system_logs)
--    Registro imutável de eventos da camada de aplicação:
--    requisições HTTP, erros, eventos de domínio, ações de segurança.
-- ---------------------------------------------------------------------------
CREATE TABLE ng_audit.ng_system_logs (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    logged_at       TIMESTAMPTZ NOT NULL    DEFAULT NOW(),
    level           VARCHAR(10) NOT NULL,          -- TRACE|DEBUG|INFO|WARN|ERROR|FATAL
    logger_name     VARCHAR(255),
    message         TEXT        NOT NULL,
    stack_trace     TEXT,
    trace_id        VARCHAR(64),                   -- distributed tracing (Micrometer/OpenTelemetry)
    span_id         VARCHAR(32),
    user_id         UUID,                          -- usuário autenticado, se disponível
    request_path    VARCHAR(512),
    http_method     VARCHAR(10),
    http_status     SMALLINT,
    duration_ms     INTEGER,
    ip_address      VARCHAR(64),
    user_agent      VARCHAR(512),
    action          VARCHAR(80),                   -- ação semântica (ex: USER_LOGIN, VAULT_OPEN)
    resource_type   VARCHAR(80),
    resource_id     TEXT,
    metadata        JSONB,
    CONSTRAINT ng_system_logs_level_check CHECK (
        level IN ('TRACE','DEBUG','INFO','WARN','ERROR','FATAL')
    )
);

CREATE INDEX idx_sys_logs_time         ON ng_audit.ng_system_logs (logged_at DESC);
CREATE INDEX idx_sys_logs_level_time   ON ng_audit.ng_system_logs (level, logged_at DESC);
CREATE INDEX idx_sys_logs_user         ON ng_audit.ng_system_logs (user_id, logged_at DESC);
CREATE INDEX idx_sys_logs_action       ON ng_audit.ng_system_logs (action, logged_at DESC);
CREATE INDEX idx_sys_logs_trace        ON ng_audit.ng_system_logs (trace_id);
CREATE INDEX idx_sys_logs_status       ON ng_audit.ng_system_logs (http_status, logged_at DESC);

-- ---------------------------------------------------------------------------
-- 3. FUNÇÃO BLOQUEADORA DE MUTAÇÕES (compartilhada pelos dois triggers)
--    Garante imutabilidade em nível de banco: impossível alterar ou apagar
--    registros de auditoria mesmo com acesso direto ao PostgreSQL.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION ng_audit.fn_block_audit_mutations()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION
        'Operação proibida: a tabela de auditoria "%" é imutável. '
        'UPDATE e DELETE são bloqueados em nível de banco (RNF-SEC.8).',
        TG_TABLE_NAME
    USING ERRCODE = '55000'; -- object_not_in_prerequisite_state
    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_ng_db_audit_immutable
BEFORE UPDATE OR DELETE ON ng_audit.ng_db_audit
FOR EACH ROW EXECUTE FUNCTION ng_audit.fn_block_audit_mutations();

CREATE TRIGGER trg_ng_system_logs_immutable
BEFORE UPDATE OR DELETE ON ng_audit.ng_system_logs
FOR EACH ROW EXECUTE FUNCTION ng_audit.fn_block_audit_mutations();

-- ---------------------------------------------------------------------------
-- 4a. FUNÇÃO GENÉRICA DE CAPTURA (ng_audit.fn_capture_dml)
--     Para tabelas sem dados sensíveis: grava old/new completos como JSONB.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION ng_audit.fn_capture_dml()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_record_id TEXT;
    v_old       JSONB;
    v_new       JSONB;
BEGIN
    IF TG_OP = 'INSERT' THEN
        v_record_id := (row_to_json(NEW) ->> 'id');
        v_new       := row_to_json(NEW)::JSONB;
    ELSIF TG_OP = 'UPDATE' THEN
        v_record_id := (row_to_json(NEW) ->> 'id');
        v_old       := row_to_json(OLD)::JSONB;
        v_new       := row_to_json(NEW)::JSONB;
    ELSIF TG_OP = 'DELETE' THEN
        v_record_id := (row_to_json(OLD) ->> 'id');
        v_old       := row_to_json(OLD)::JSONB;
    END IF;

    INSERT INTO ng_audit.ng_db_audit
        (operation, schema_name, table_name, record_id, old_data, new_data, client_addr)
    VALUES
        (TG_OP, TG_TABLE_SCHEMA, TG_TABLE_NAME, v_record_id, v_old, v_new,
         inet_client_addr());

    RETURN COALESCE(NEW, OLD);
END;
$$;

-- ---------------------------------------------------------------------------
-- 4b. FUNÇÃO REDACTADA (ng_audit.fn_capture_dml_redacted)
--     Para tabelas de credenciais: remove colunas sensíveis antes de gravar.
--     Registra apenas: id, user_id, timestamps e flags públicas.
--     As colunas removidas nunca chegam ao log — nem como hash nem cifradas.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION ng_audit.fn_capture_dml_redacted()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    -- Colunas de credencial a NUNCA persistir no audit log
    SENSITIVE_COLS CONSTANT TEXT[] := ARRAY[
        'password_hash',      -- ng_users
        'refresh_token_hash', -- ng_sessions
        'token_hash',         -- ng_sessions, ng_password_resets, ng_email_verifications
        'encrypted_secret'    -- ng_totp_secrets
    ];
    v_record_id TEXT;
    v_old       JSONB;
    v_new       JSONB;
    col         TEXT;
BEGIN
    IF TG_OP = 'INSERT' THEN
        v_record_id := (row_to_json(NEW) ->> 'id');
        v_new       := row_to_json(NEW)::JSONB;
    ELSIF TG_OP = 'UPDATE' THEN
        v_record_id := (row_to_json(NEW) ->> 'id');
        v_old       := row_to_json(OLD)::JSONB;
        v_new       := row_to_json(NEW)::JSONB;
    ELSIF TG_OP = 'DELETE' THEN
        v_record_id := (row_to_json(OLD) ->> 'id');
        v_old       := row_to_json(OLD)::JSONB;
    END IF;

    -- Remover colunas sensíveis de ambos os snapshots
    FOREACH col IN ARRAY SENSITIVE_COLS LOOP
        v_old := v_old - col;
        v_new := v_new - col;
    END LOOP;

    INSERT INTO ng_audit.ng_db_audit
        (operation, schema_name, table_name, record_id, old_data, new_data, client_addr)
    VALUES
        (TG_OP, TG_TABLE_SCHEMA, TG_TABLE_NAME, v_record_id, v_old, v_new,
         inet_client_addr());

    RETURN COALESCE(NEW, OLD);
END;
$$;

-- ---------------------------------------------------------------------------
-- 5. TRIGGERS NAS TABELAS CRÍTICAS
-- ---------------------------------------------------------------------------

-- ng_identity — tabelas de CREDENCIAIS usam a função redactada
CREATE TRIGGER trg_audit_ng_users
AFTER INSERT OR UPDATE OR DELETE ON ng_identity.ng_users
FOR EACH ROW EXECUTE FUNCTION ng_audit.fn_capture_dml_redacted();

CREATE TRIGGER trg_audit_ng_sessions
AFTER INSERT OR UPDATE OR DELETE ON ng_identity.ng_sessions
FOR EACH ROW EXECUTE FUNCTION ng_audit.fn_capture_dml_redacted();

CREATE TRIGGER trg_audit_ng_totp_secrets
AFTER INSERT OR UPDATE OR DELETE ON ng_identity.ng_totp_secrets
FOR EACH ROW EXECUTE FUNCTION ng_audit.fn_capture_dml_redacted();

CREATE TRIGGER trg_audit_ng_password_resets
AFTER INSERT OR UPDATE OR DELETE ON ng_identity.ng_password_resets
FOR EACH ROW EXECUTE FUNCTION ng_audit.fn_capture_dml_redacted();

CREATE TRIGGER trg_audit_ng_consent_records
AFTER INSERT OR UPDATE OR DELETE ON ng_identity.ng_consent_records
FOR EACH ROW EXECUTE FUNCTION ng_audit.fn_capture_dml();

CREATE TRIGGER trg_audit_ng_audit_logs
AFTER INSERT OR UPDATE OR DELETE ON ng_identity.ng_audit_logs
FOR EACH ROW EXECUTE FUNCTION ng_audit.fn_capture_dml();

-- ng_core
CREATE TRIGGER trg_audit_ng_support_links
AFTER INSERT OR UPDATE OR DELETE ON ng_core.ng_support_links
FOR EACH ROW EXECUTE FUNCTION ng_audit.fn_capture_dml();

CREATE TRIGGER trg_audit_ng_link_permissions
AFTER INSERT OR UPDATE OR DELETE ON ng_core.ng_link_permissions
FOR EACH ROW EXECUTE FUNCTION ng_audit.fn_capture_dml();

CREATE TRIGGER trg_audit_ng_events
AFTER INSERT OR UPDATE OR DELETE ON ng_core.ng_events
FOR EACH ROW EXECUTE FUNCTION ng_audit.fn_capture_dml();

CREATE TRIGGER trg_audit_ng_tasks
AFTER INSERT OR UPDATE OR DELETE ON ng_core.ng_tasks
FOR EACH ROW EXECUTE FUNCTION ng_audit.fn_capture_dml();

CREATE TRIGGER trg_audit_ng_focus_sessions
AFTER INSERT OR UPDATE OR DELETE ON ng_core.ng_focus_sessions
FOR EACH ROW EXECUTE FUNCTION ng_audit.fn_capture_dml();

-- ng_sensitive (apenas metadados — ciphertext permanece cifrado no JSONB)
CREATE TRIGGER trg_audit_ng_mood_checkins
AFTER INSERT OR UPDATE OR DELETE ON ng_sensitive.ng_mood_checkins
FOR EACH ROW EXECUTE FUNCTION ng_audit.fn_capture_dml();
