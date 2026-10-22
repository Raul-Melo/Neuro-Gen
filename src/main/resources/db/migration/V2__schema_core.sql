-- Schema ng_core — dados operacionais (vínculos, agenda, foco, notificações)
CREATE SCHEMA IF NOT EXISTS ng_core;

CREATE TABLE ng_core.ng_support_links (
    id                UUID PRIMARY KEY,
    titular_id        UUID        NOT NULL REFERENCES ng_identity.ng_users (id),
    apoio_id          UUID        REFERENCES ng_identity.ng_users (id),
    invite_token_hash VARCHAR(128) NOT NULL UNIQUE,
    invite_expires_at TIMESTAMPTZ NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    accepted_at       TIMESTAMPTZ,
    revoked_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE ng_core.ng_link_permissions (
    id          UUID PRIMARY KEY,
    link_id     UUID        NOT NULL REFERENCES ng_core.ng_support_links (id),
    module_name VARCHAR(40) NOT NULL,
    granted_by  UUID        NOT NULL REFERENCES ng_identity.ng_users (id),
    granted_at  TIMESTAMPTZ NOT NULL
);

CREATE TABLE ng_core.ng_user_profiles (
    id                        UUID PRIMARY KEY REFERENCES ng_identity.ng_users (id),
    display_name              VARCHAR(120),
    timezone                  VARCHAR(60) DEFAULT 'America/Sao_Paulo',
    locale                    VARCHAR(10) DEFAULT 'pt-BR',
    neurodivergence_profile   VARCHAR(40)[],
    prefer_not_inform_profile BOOLEAN DEFAULT FALSE,
    sensory_preferences       JSONB,
    onboarding_completed      BOOLEAN DEFAULT FALSE,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE ng_core.ng_categories (
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES ng_identity.ng_users (id),
    name       VARCHAR(80) NOT NULL,
    color      VARCHAR(7)  NOT NULL,
    icon       VARCHAR(60),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE ng_core.ng_events (
    id                  UUID PRIMARY KEY,
    owner_id            UUID         NOT NULL REFERENCES ng_identity.ng_users (id),
    title               VARCHAR(200) NOT NULL,
    description         TEXT,
    start_at            TIMESTAMPTZ  NOT NULL,
    end_at              TIMESTAMPTZ  NOT NULL,
    event_type          VARCHAR(20)  NOT NULL,
    rrule               VARCHAR(255),
    category_id         UUID         REFERENCES ng_core.ng_categories (id),
    shared_with_support BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    cancelled_at        TIMESTAMPTZ
);

CREATE TABLE ng_core.ng_tasks (
    id                  UUID PRIMARY KEY,
    owner_id            UUID         NOT NULL REFERENCES ng_identity.ng_users (id),
    title               VARCHAR(200) NOT NULL,
    description         TEXT,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    priority            VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    due_date            DATE,
    category_id         UUID         REFERENCES ng_core.ng_categories (id),
    shared_with_support BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    archived_at         TIMESTAMPTZ
);

CREATE TABLE ng_core.ng_subtasks (
    id           UUID PRIMARY KEY,
    task_id      UUID         NOT NULL REFERENCES ng_core.ng_tasks (id),
    title        VARCHAR(200) NOT NULL,
    done         BOOLEAN      NOT NULL DEFAULT FALSE,
    position     INTEGER      NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE ng_core.ng_reminders (
    id                  UUID PRIMARY KEY,
    event_id            UUID REFERENCES ng_core.ng_events (id),
    task_id             UUID REFERENCES ng_core.ng_tasks (id),
    minutes_before      INTEGER     NOT NULL,
    persistent          BOOLEAN     NOT NULL DEFAULT FALSE,
    repeat_interval_min INTEGER,
    max_repetitions     INTEGER,
    current_repetitions INTEGER     NOT NULL DEFAULT 0,
    status              VARCHAR(30) NOT NULL,
    next_fire_at        TIMESTAMPTZ,
    confirmation_at     TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ng_reminders_event_xor_task CHECK (
        (event_id IS NOT NULL AND task_id IS NULL) OR (event_id IS NULL AND task_id IS NOT NULL)
    )
);

CREATE TABLE ng_core.ng_focus_sessions (
    id                UUID PRIMARY KEY,
    user_id           UUID        NOT NULL REFERENCES ng_identity.ng_users (id),
    task_id           UUID,
    planned_minutes   INTEGER     NOT NULL,
    break_minutes     INTEGER     NOT NULL DEFAULT 5,
    status            VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    started_at        TIMESTAMPTZ NOT NULL,
    ended_at          TIMESTAMPTZ,
    effective_seconds BIGINT      NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE ng_core.ng_focus_pauses (
    id         UUID PRIMARY KEY,
    session_id UUID        NOT NULL REFERENCES ng_core.ng_focus_sessions (id),
    paused_at  TIMESTAMPTZ NOT NULL,
    resumed_at TIMESTAMPTZ
);

CREATE TABLE ng_core.ng_notification_channels (
    id            UUID PRIMARY KEY,
    user_id       UUID        NOT NULL REFERENCES ng_identity.ng_users (id),
    channel_type  VARCHAR(20) NOT NULL,
    endpoint      VARCHAR(512) NOT NULL,
    active        BOOLEAN     NOT NULL DEFAULT TRUE,
    silence_start TIME,
    silence_end   TIME,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE ng_core.ng_notification_logs (
    id             UUID PRIMARY KEY,
    user_id        UUID        NOT NULL REFERENCES ng_identity.ng_users (id),
    reminder_id    UUID REFERENCES ng_core.ng_reminders (id),
    channel_type   VARCHAR(20) NOT NULL,
    status         VARCHAR(20) NOT NULL,
    sent_at        TIMESTAMPTZ,
    delivered_at   TIMESTAMPTZ,
    confirmed_at   TIMESTAMPTZ,
    failure_reason VARCHAR(255),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
