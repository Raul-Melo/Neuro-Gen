-- Schema ng_sensitive — dados sensíveis de saúde (LGPD art. 5º, II / ADR-006)
-- Conteúdo cifrado em repouso (AES-256-GCM) na camada de aplicação, armazenado como BYTEA.
CREATE SCHEMA IF NOT EXISTS ng_sensitive;

CREATE TABLE ng_sensitive.ng_mood_checkins (
    id           UUID PRIMARY KEY,
    user_id      UUID       NOT NULL REFERENCES ng_identity.ng_users (id),
    mood_score   SMALLINT   NOT NULL,
    energy_level SMALLINT   NOT NULL,
    notes_enc    BYTEA,
    checkin_date DATE       NOT NULL,
    checkin_time TIME       NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE ng_sensitive.ng_journal_entries (
    id                 UUID PRIMARY KEY,
    user_id            UUID       NOT NULL REFERENCES ng_identity.ng_users (id),
    content_enc        BYTEA      NOT NULL,
    related_checkin_id UUID       REFERENCES ng_sensitive.ng_mood_checkins (id),
    entry_date         DATE       NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE ng_sensitive.ng_neurodivergence_profiles (
    id                UUID PRIMARY KEY,
    user_id           UUID       NOT NULL UNIQUE REFERENCES ng_identity.ng_users (id),
    declared_profiles VARCHAR(40)[],
    prefer_not_inform BOOLEAN    NOT NULL DEFAULT FALSE,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
