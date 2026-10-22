-- Índices de performance (RNF-ESC.4 — alvo 100k MAU sem redesenho)
CREATE INDEX idx_sessions_user_revoked   ON ng_identity.ng_sessions (user_id, revoked_at);
CREATE INDEX idx_sessions_expires        ON ng_identity.ng_sessions (expires_at);

CREATE INDEX idx_links_titular_status    ON ng_core.ng_support_links (titular_id, status);
CREATE INDEX idx_links_apoio_status      ON ng_core.ng_support_links (apoio_id, status);
CREATE INDEX idx_link_perms_link         ON ng_core.ng_link_permissions (link_id);

CREATE INDEX idx_events_owner_period     ON ng_core.ng_events (owner_id, start_at, end_at);
CREATE INDEX idx_tasks_owner_status_due  ON ng_core.ng_tasks (owner_id, status, due_date);
CREATE INDEX idx_subtasks_task_position  ON ng_core.ng_subtasks (task_id, position);
CREATE INDEX idx_reminders_status_fire   ON ng_core.ng_reminders (status, next_fire_at);
CREATE INDEX idx_focus_user_status       ON ng_core.ng_focus_sessions (user_id, status);

CREATE INDEX idx_mood_user_date          ON ng_sensitive.ng_mood_checkins (user_id, checkin_date DESC);
