# Auditoria — Referência Técnica

> **Schema:** `ng_audit`  
> **Alimentado por:** Triggers PostgreSQL (síncrono) + `AuditService` Java (`@Async`)  
> **Requisitos:** RNF-SEC.8 / RNF-PRIV.8 / LGPD art. 37  
> **Imutabilidade:** `UPDATE` e `DELETE` lançam `SQLSTATE 55000` em nível de banco

---

## Visão Geral

O sistema mantém dois registros de auditoria complementares:

| Tabela | Alimentada por | Timing | Escopo |
|---|---|---|---|
| `ng_audit.ng_db_audit` | Triggers PostgreSQL (`fn_capture_dml`, `fn_capture_dml_redacted`) | Síncrono (mesma transação) | INSERT/UPDATE/DELETE nas tabelas críticas |
| `ng_audit.ng_system_logs` | `AuditService.java` via `SystemLogPort` | Assíncrono (`@Async`) | Eventos de domínio, requisições HTTP, erros, eventos de segurança |

> **Garantia de imutabilidade:** a função `ng_audit.fn_block_audit_mutations()` está instalada como trigger BEFORE UPDATE OR DELETE em ambas as tabelas. Nenhum usuário do banco, incluindo o próprio app, consegue alterar ou apagar um registro de auditoria já gravado.

---

## Tabela: `ng_audit.ng_db_audit`

Registra automaticamente toda operação DML nas tabelas críticas. Alimentada por triggers individuais, **sem nenhuma chamada de código Java** — garante cobertura mesmo com acesso direto via psql ou migrations.

### Colunas

| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | uuid PK | Gerado automaticamente (`gen_random_uuid()`) |
| `occurred_at` | timestamptz | Momento exato da operação (`NOW()`) |
| `operation` | varchar(10) | `INSERT`, `UPDATE` ou `DELETE` |
| `schema_name` | varchar(63) | Schema da tabela afetada |
| `table_name` | varchar(63) | Tabela afetada |
| `record_id` | text | Valor da coluna `id` da linha afetada |
| `old_data` | jsonb | Snapshot antes da operação (UPDATE/DELETE) |
| `new_data` | jsonb | Snapshot depois da operação (INSERT/UPDATE) |
| `db_user` | varchar(128) | Usuário PostgreSQL que executou (`current_user`) |
| `app_user_id` | uuid | ID do usuário JWT, se propagado via `SET LOCAL app.current_user_id` |
| `client_addr` | inet | Endereço IP do cliente de banco (`inet_client_addr()`) |
| `tx_id` | bigint | ID da transação PostgreSQL (`txid_current()`) |

### Proteção de dados sensíveis

A função `fn_capture_dml_redacted` remove automaticamente as seguintes colunas antes de gravar no audit log, **mesmo que a migration seja rodada manualmente**:

| Coluna removida | Tabela de origem |
|---|---|
| `password_hash` | `ng_identity.ng_users` |
| `refresh_token_hash` | `ng_identity.ng_sessions` |
| `token_hash` | `ng_identity.ng_sessions`, `ng_password_resets`, `ng_email_verifications` |
| `encrypted_secret` | `ng_identity.ng_totp_secrets` |

---

## Tabela: `ng_audit.ng_system_logs`

Registra eventos gerados pela camada de aplicação. Escrita assíncrona — falha ao persistir **não reverte** a operação original.

### Colunas

| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | uuid PK | Gerado automaticamente |
| `logged_at` | timestamptz | Momento do log |
| `level` | varchar(10) | `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `FATAL` |
| `logger_name` | varchar(255) | Classe Java que gerou o log |
| `message` | text | Mensagem descritiva |
| `stack_trace` | text | Stack trace (somente `ERROR`/`FATAL`), truncado em 8.000 chars |
| `trace_id` | varchar(64) | ID de rastreamento distribuído (Micrometer/OpenTelemetry) |
| `span_id` | varchar(32) | Span ID do trace |
| `user_id` | uuid | Usuário autenticado no momento do evento |
| `request_path` | varchar(512) | Path HTTP da requisição |
| `http_method` | varchar(10) | Método HTTP (`GET`, `POST`, etc.) |
| `http_status` | smallint | Código de resposta HTTP |
| `duration_ms` | integer | Tempo de resposta em milissegundos |
| `ip_address` | varchar(64) | IP do cliente HTTP |
| `user_agent` | varchar(512) | User-Agent do cliente |
| `action` | varchar(80) | Ação semântica (ver tabela de ações abaixo) |
| `resource_type` | varchar(80) | Tipo do recurso afetado (ex: `SupportLink`, `VaultEntry`) |
| `resource_id` | text | ID do recurso afetado |
| `metadata` | jsonb | Dados adicionais contextuais |

### Ações registradas (`action`)

| Valor de `action` | Método | Nível | Descrição |
|---|---|---|---|
| `USER_REGISTERED` | `logAction` | INFO | Novo usuário registrado |
| `USER_LOGIN` | `logSecurity` | INFO/WARN | Login bem-sucedido ou falho |
| `USER_LOGOUT` | `logSecurity` | INFO | Logout e revogação do refresh token |
| `EMAIL_VERIFIED` | `logAction` | INFO | E-mail confirmado |
| `PASSWORD_RESET_REQUESTED` | `logAction` | INFO | Solicitação de redefinição de senha |
| `PASSWORD_RESET_CONFIRMED` | `logAction` | INFO | Senha redefinida com sucesso |
| `TOTP_ENROLLED` | `logSecurity` | INFO | 2FA ativado |
| `TOTP_FAILED` | `logSecurity` | WARN | Código TOTP inválido |
| `LINK_CREATED` | `logAction` | INFO | Vínculo de suporte criado |
| `LINK_ACCEPTED` | `logAction` | INFO | Convite aceito |
| `LINK_REVOKED` | `logAction` | INFO | Vínculo revogado |
| `VAULT_OPENED` | `logSecurity` | INFO | Cofre aberto pelo Titular |
| `VAULT_CLOSED` | `logSecurity` | INFO | Cofre fechado |
| `VAULT_ACCESS_DENIED_APOIO` | `logSecurity` | WARN | Tentativa de acesso ao Cofre por conta APOIO |
| `VAULT_ENTRY_CREATED` | `logAction` | INFO | Nova entrada no Cofre |
| `VAULT_ENTRY_DELETED` | `logAction` | INFO | Entrada removida do Cofre |

---

## Como o `AuditService` é usado no código

```java
@Service
public class ExemploUseCase {

    private final AuditService audit;

    // Log de ação de domínio
    public void executar(UUID userId) {
        // ... lógica de negócio ...
        audit.logAction("LINK_CREATED", userId, "SupportLink", linkId.toString(),
                Map.of("apoio_email", apoioEmail));
    }

    // Log de evento de segurança
    public void login(String ip, UUID userId, boolean success) {
        audit.logSecurity("USER_LOGIN", userId, ip, success,
                Map.of("method", "password"));
    }

    // Log de requisição HTTP (tipicamente em um HandlerInterceptor)
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, long duration) {
        audit.logHttp(req.getMethod(), req.getRequestURI(), res.getStatus(),
                duration, currentUserId, req.getRemoteAddr(), req.getHeader("User-Agent"));
    }
}
```

> `AuditService` é um `@Service` Spring. Injete-o diretamente nos use cases ou em interceptors.  
> Todos os métodos são `@Async` — retornam imediatamente sem bloquear a thread da requisição.

---

## Consultas de Referência

### Últimas 50 operações de banco

```sql
SELECT occurred_at, operation, schema_name, table_name, record_id, app_user_id
FROM ng_audit.ng_db_audit
ORDER BY occurred_at DESC
LIMIT 50;
```

### Histórico completo de um registro específico

```sql
SELECT occurred_at, operation, old_data, new_data, app_user_id
FROM ng_audit.ng_db_audit
WHERE table_name = 'ng_users'
  AND record_id = '<uuid-do-usuario>'
ORDER BY occurred_at;
```

### Operações de um usuário nas últimas 24h

```sql
SELECT occurred_at, operation, table_name, record_id
FROM ng_audit.ng_db_audit
WHERE app_user_id = '<uuid-do-usuario>'
  AND occurred_at > NOW() - INTERVAL '24 hours'
ORDER BY occurred_at DESC;
```

### Eventos de segurança com falha

```sql
SELECT logged_at, action, user_id, ip_address, metadata
FROM ng_audit.ng_system_logs
WHERE level = 'WARN'
  AND action IS NOT NULL
ORDER BY logged_at DESC
LIMIT 50;
```

### Tentativas de acesso ao Cofre por contas APOIO

```sql
SELECT logged_at, user_id, ip_address, metadata
FROM ng_audit.ng_system_logs
WHERE action = 'VAULT_ACCESS_DENIED_APOIO'
ORDER BY logged_at DESC;
```

### Rastrear uma requisição por trace_id

```sql
SELECT logged_at, level, action, http_method, request_path, http_status,
       duration_ms, user_id, message
FROM ng_audit.ng_system_logs
WHERE trace_id = '<trace-id>'
ORDER BY logged_at;
```

### Erros das últimas 2 horas

```sql
SELECT logged_at, message, stack_trace, user_id, request_path
FROM ng_audit.ng_system_logs
WHERE level IN ('ERROR', 'FATAL')
  AND logged_at > NOW() - INTERVAL '2 hours'
ORDER BY logged_at DESC;
```

### Volume de operações por tabela (última semana)

```sql
SELECT table_name, operation, COUNT(*) AS total
FROM ng_audit.ng_db_audit
WHERE occurred_at > NOW() - INTERVAL '7 days'
GROUP BY table_name, operation
ORDER BY total DESC;
```

---

## Tabelas auditadas (cobertura dos triggers)

| Tabela | Função trigger | Colunas removidas |
|---|---|---|
| `ng_identity.ng_users` | `fn_capture_dml_redacted` | `password_hash` |
| `ng_identity.ng_sessions` | `fn_capture_dml_redacted` | `refresh_token_hash`, `token_hash` |
| `ng_identity.ng_totp_secrets` | `fn_capture_dml_redacted` | `encrypted_secret` |
| `ng_identity.ng_password_resets` | `fn_capture_dml_redacted` | `token_hash` |
| `ng_identity.ng_consent_records` | `fn_capture_dml` | — |
| `ng_identity.ng_audit_logs` | `fn_capture_dml` | — |
| `ng_core.ng_support_links` | `fn_capture_dml` | — |
| `ng_core.ng_link_permissions` | `fn_capture_dml` | — |
| `ng_core.ng_events` | `fn_capture_dml` | — |
| `ng_core.ng_tasks` | `fn_capture_dml` | — |
| `ng_core.ng_focus_sessions` | `fn_capture_dml` | — |
| `ng_sensitive.ng_mood_checkins` | `fn_capture_dml` | — |

> **Nota sobre `ng_sensitive`:** o conteúdo de `notes_enc` e `content_enc` já é armazenado criptografado. O trigger grava o ciphertext no log — sem chave de decriptação, o dado permanece ininteligível.

---

## Verificar imutabilidade

Tente forçar uma mutação para confirmar que o bloqueio funciona:

```sql
-- Deve lançar: ERROR 55000: Operação proibida: a tabela de auditoria "ng_db_audit" é imutável
UPDATE ng_audit.ng_db_audit SET operation = 'TESTE' WHERE id = (SELECT id FROM ng_audit.ng_db_audit LIMIT 1);

-- Deve lançar o mesmo erro
DELETE FROM ng_audit.ng_system_logs WHERE id = (SELECT id FROM ng_audit.ng_system_logs LIMIT 1);
```

---

## Índices disponíveis

| Tabela | Índice | Uso típico |
|---|---|---|
| `ng_db_audit` | `(schema_name, table_name, occurred_at DESC)` | Histórico de uma tabela |
| `ng_db_audit` | `(app_user_id, occurred_at DESC)` | Auditoria por usuário |
| `ng_db_audit` | `(table_name, record_id)` | Histórico de um registro |
| `ng_db_audit` | `(tx_id)` | Rastrear transação completa |
| `ng_system_logs` | `(logged_at DESC)` | Log mais recente |
| `ng_system_logs` | `(level, logged_at DESC)` | Filtrar por severidade |
| `ng_system_logs` | `(user_id, logged_at DESC)` | Histórico de um usuário |
| `ng_system_logs` | `(action, logged_at DESC)` | Eventos de um tipo específico |
| `ng_system_logs` | `(trace_id)` | Rastreamento distribuído |
| `ng_system_logs` | `(http_status, logged_at DESC)` | Análise de erros HTTP |
