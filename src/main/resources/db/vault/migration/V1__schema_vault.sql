-- Banco/schema ISOLADO do Vault (ADR-005). Datasource e Flyway próprios.
-- O servidor armazena apenas bytes cifrados (zero-knowledge, RNF-SEC.3).
CREATE SCHEMA IF NOT EXISTS ng_vault;

CREATE TABLE ng_vault.ng_vault_entries (
    id         UUID PRIMARY KEY,
    owner_id   UUID         NOT NULL,   -- id do Titular (sem FK cross-DB — isolamento)
    label      VARCHAR(200) NOT NULL,
    entry_type VARCHAR(30)  NOT NULL,
    ciphertext BYTEA        NOT NULL,
    iv         BYTEA        NOT NULL,
    salt       BYTEA        NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE ng_vault.ng_vault_auth_logs (
    id              UUID PRIMARY KEY,
    user_id         UUID        NOT NULL,
    action          VARCHAR(40) NOT NULL,
    ip_address      VARCHAR(64),
    user_agent_hash VARCHAR(128),
    success         BOOLEAN     NOT NULL,
    failure_reason  VARCHAR(255),
    attempted_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- master_verifier_hash: verificador da senha mestra derivada no cliente (extensão do DER para
-- permitir validar a abertura do Cofre sem armazenar a chave — mantém o zero-knowledge).
CREATE TABLE ng_vault.ng_master_key_meta (
    id                   UUID PRIMARY KEY,
    user_id              UUID         NOT NULL UNIQUE,
    argon2_params        VARCHAR(100) NOT NULL,
    master_verifier_hash VARCHAR(128) NOT NULL,
    recovery_key_hash    VARCHAR(128) NOT NULL,
    configured_at        TIMESTAMPTZ  NOT NULL,
    last_auth_at         TIMESTAMPTZ
);

CREATE INDEX idx_vault_entries_owner ON ng_vault.ng_vault_entries (owner_id);
CREATE INDEX idx_vault_auth_user     ON ng_vault.ng_vault_auth_logs (user_id, attempted_at DESC);
