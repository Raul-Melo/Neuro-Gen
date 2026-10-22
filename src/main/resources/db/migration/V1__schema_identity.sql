-- Schema ng_identity — autenticação, sessões e auditoria (DER 02-der.md)
CREATE SCHEMA IF NOT EXISTS ng_identity;

CREATE TABLE ng_identity.ng_users (
    id             UUID PRIMARY KEY,
    email          VARCHAR(254) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    role           VARCHAR(20)  NOT NULL,
    status         VARCHAR(30)  NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    totp_enabled   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at     TIMESTAMPTZ
);

CREATE TABLE ng_identity.ng_sessions (
    id                 UUID PRIMARY KEY,
    user_id            UUID        NOT NULL REFERENCES ng_identity.ng_users (id),
    refresh_token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at         TIMESTAMPTZ NOT NULL,
    device_info        VARCHAR(512),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at         TIMESTAMPTZ
);

CREATE TABLE ng_identity.ng_email_verifications (
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES ng_identity.ng_users (id),
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE ng_identity.ng_password_resets (
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES ng_identity.ng_users (id),
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE ng_identity.ng_totp_secrets (
    id               UUID PRIMARY KEY,
    user_id          UUID        NOT NULL UNIQUE REFERENCES ng_identity.ng_users (id),
    encrypted_secret VARCHAR(512) NOT NULL,
    enabled_at       TIMESTAMPTZ NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Tabelas de compliance/auditoria (DER) — criadas para rastreabilidade (RNF-PRIV.8 / RNF-SEC.8).
CREATE TABLE ng_identity.ng_consent_records (
    id                     UUID PRIMARY KEY,
    user_id                UUID        NOT NULL REFERENCES ng_identity.ng_users (id),
    privacy_policy_version VARCHAR(40) NOT NULL,
    terms_version          VARCHAR(40) NOT NULL,
    ip_address             VARCHAR(64) NOT NULL,
    consented_at           TIMESTAMPTZ NOT NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE ng_identity.ng_audit_logs (
    id              UUID PRIMARY KEY,
    user_id         UUID,
    action          VARCHAR(80) NOT NULL,
    resource_type   VARCHAR(80),
    resource_id     VARCHAR(80),
    ip_address      VARCHAR(64),
    user_agent_hash VARCHAR(128),
    metadata        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
