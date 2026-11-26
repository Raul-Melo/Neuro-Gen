-- =============================================================================
-- NEURO-GEN — DADOS DE DEMONSTRAÇÃO COMPLETOS
-- Executa via: docker exec -i neurogen-postgres psql -U neurogen -d neurogen < scripts/demo-data.sql
-- UUIDs válidos: formato 8-4-4-4-12 (todos hex)
-- =============================================================================

\set ON_ERROR_STOP on
\echo '=== Limpando dados de demo anteriores ==='

DELETE FROM ng_vault.ng_vault_auth_logs   WHERE user_id::text LIKE 'a1b2c3d4%' OR user_id::text LIKE 'b2c3d4e5%';
DELETE FROM ng_audit.ng_system_logs        WHERE ip_address IN ('177.42.100.55','192.168.1.10','200.100.50.20','10.0.0.99','203.45.67.89');
DELETE FROM ng_identity.ng_audit_logs      WHERE id::text LIKE 'a10b0001%' OR id::text LIKE 'a10b0002%' OR id::text LIKE 'a10b0003%' OR id::text LIKE 'a10b0004%' OR id::text LIKE 'a10b0005%';
DELETE FROM ng_sensitive.ng_mood_checkins  WHERE id::text LIKE 'b00dc001%' OR id::text LIKE 'b00dc002%' OR id::text LIKE 'b00dc003%';
DELETE FROM ng_core.ng_focus_pauses        WHERE id::text LIKE 'fa0ce001%';
DELETE FROM ng_core.ng_focus_sessions      WHERE id::text LIKE 'f0c5e001%';
DELETE FROM ng_core.ng_reminders           WHERE id::text LIKE 'ae010001%' OR id::text LIKE 'ae010002%';
DELETE FROM ng_core.ng_subtasks            WHERE id::text LIKE '5ab10001%' OR id::text LIKE '5ab10002%' OR id::text LIKE '5ab10003%' OR id::text LIKE '5ab10004%';
DELETE FROM ng_core.ng_tasks               WHERE id::text LIKE 'ba5c0001%' OR id::text LIKE 'ba5c0002%' OR id::text LIKE 'ba5c0003%';
DELETE FROM ng_core.ng_events              WHERE id::text LIKE 'e0e10001%' OR id::text LIKE 'e0e10002%' OR id::text LIKE 'e0e10003%';
DELETE FROM ng_core.ng_link_permissions    WHERE id::text LIKE 'be110001%' OR id::text LIKE 'be110002%' OR id::text LIKE 'be110003%' OR id::text LIKE 'be110004%';
DELETE FROM ng_core.ng_support_links       WHERE id::text LIKE '11c10001%' OR id::text LIKE '11c10002%' OR id::text LIKE '11c10003%';
DELETE FROM ng_core.ng_categories          WHERE id::text LIKE 'ca710001%' OR id::text LIKE 'ca710002%' OR id::text LIKE 'ca710003%';
DELETE FROM ng_sensitive.ng_neurodivergence_profiles WHERE id::text LIKE 'ffe00001%' OR id::text LIKE 'ffe00002%';
DELETE FROM ng_core.ng_user_profiles       WHERE id = 'a1b2c3d4-0001-0001-0001-000000000001' OR id = 'a1b2c3d4-0002-0002-0002-000000000002' OR id = 'a1b2c3d4-0003-0003-0003-000000000003';
DELETE FROM ng_identity.ng_consent_records WHERE id::text LIKE 'c0115001%' OR id::text LIKE 'c0115002%' OR id::text LIKE 'c0115003%';
DELETE FROM ng_identity.ng_sessions        WHERE id::text LIKE '5e550001%' OR id::text LIKE '5e550002%' OR id::text LIKE '5e550003%';
DELETE FROM ng_identity.ng_users           WHERE email LIKE '%@neurogen.demo';

\echo '=== BLOCO 1: USUARIOS ==='

INSERT INTO ng_identity.ng_users (id, email, password_hash, role, status, email_verified, totp_enabled) VALUES
('a1b2c3d4-0001-0001-0001-000000000001','lucas.oliveira@neurogen.demo',   'argon2id:demo:lucas:hash',   'TITULAR','ACTIVE',               TRUE,  FALSE),
('a1b2c3d4-0002-0002-0002-000000000002','sofia.martins@neurogen.demo',    'argon2id:demo:sofia:hash',   'TITULAR','ACTIVE',               TRUE,  FALSE),
('a1b2c3d4-0003-0003-0003-000000000003','pedro.alves@neurogen.demo',      'argon2id:demo:pedro:hash',   'TITULAR','ACTIVE',               TRUE,  TRUE),
('b2c3d4e5-0004-0004-0004-000000000004','ana.lima@neurogen.demo',         'argon2id:demo:ana:hash',     'APOIO',  'ACTIVE',               TRUE,  FALSE),
('b2c3d4e5-0005-0005-0005-000000000005','roberto.souza@neurogen.demo',    'argon2id:demo:roberto:hash', 'APOIO',  'ACTIVE',               TRUE,  FALSE),
('c3d4e5f6-0006-0006-0006-000000000006','conta.suspensa@neurogen.demo',   'argon2id:demo:susp:hash',    'TITULAR','SUSPENDED',            FALSE, FALSE),
('c3d4e5f6-0007-0007-0007-000000000007','conta.pendente@neurogen.demo',   'argon2id:demo:pend:hash',    'TITULAR','PENDING_VERIFICATION', FALSE, FALSE);

\echo '=== BLOCO 2: SESSOES, CONSENTIMENTO, PERFIS ==='

-- UUIDs de sessão: 5e550001-XXXX-XXXX-XXXX-XXXXXXXXXXXX
INSERT INTO ng_identity.ng_sessions (id, user_id, refresh_token_hash, expires_at, device_info) VALUES
('5e550001-0001-0001-0001-000000000001','a1b2c3d4-0001-0001-0001-000000000001','sha256_lucas_sessao_ativa_01',   NOW() + INTERVAL '30 days','Chrome 120 / Windows 11'),
('5e550002-0001-0001-0001-000000000002','a1b2c3d4-0002-0002-0002-000000000002','sha256_sofia_sessao_ativa_01',   NOW() + INTERVAL '30 days','Safari / iOS 17');

INSERT INTO ng_identity.ng_sessions (id, user_id, refresh_token_hash, expires_at, device_info, revoked_at) VALUES
('5e550003-0001-0001-0001-000000000003','a1b2c3d4-0001-0001-0001-000000000001','sha256_lucas_sessao_revogada',NOW() + INTERVAL '30 days','Firefox 119 / Android', NOW() - INTERVAL '5 days');

-- UUIDs de consent: c0115001-XXXX-XXXX-XXXX-XXXXXXXXXXXX
INSERT INTO ng_identity.ng_consent_records (id, user_id, privacy_policy_version, terms_version, ip_address, consented_at) VALUES
('c0115001-0001-0001-0001-000000000001','a1b2c3d4-0001-0001-0001-000000000001','v2.1','v3.0','177.42.100.55', NOW() - INTERVAL '30 days'),
('c0115002-0001-0001-0001-000000000002','a1b2c3d4-0002-0002-0002-000000000002','v2.1','v3.0','200.150.75.30', NOW() - INTERVAL '15 days'),
('c0115003-0001-0001-0001-000000000003','a1b2c3d4-0003-0003-0003-000000000003','v2.1','v3.0','191.5.200.10',  NOW() - INTERVAL '7 days');

INSERT INTO ng_core.ng_user_profiles (id, display_name, timezone, neurodivergence_profile, sensory_preferences, onboarding_completed) VALUES
('a1b2c3d4-0001-0001-0001-000000000001','Lucas Oliveira','America/Sao_Paulo', ARRAY['TDAH'],            '{"reduce_animations":true,"high_contrast":false,"font_size":"large"}', TRUE),
('a1b2c3d4-0002-0002-0002-000000000002','Sofia Martins', 'America/Sao_Paulo', ARRAY['TEA'],             '{"reduce_animations":true,"high_contrast":true,"sound_alerts":false}',  TRUE),
('a1b2c3d4-0003-0003-0003-000000000003','Pedro Alves',   'America/Recife',    ARRAY['SUPERDOTACAO'],    '{"reduce_animations":false,"high_contrast":false}',                      TRUE);

-- UUIDs neurodiv: ffe00001-XXXX-XXXX-XXXX-XXXXXXXXXXXX
INSERT INTO ng_sensitive.ng_neurodivergence_profiles (id, user_id, declared_profiles, prefer_not_inform) VALUES
('ffe00001-0001-0001-0001-000000000001','a1b2c3d4-0001-0001-0001-000000000001', ARRAY['TDAH'], FALSE),
('ffe00002-0001-0001-0001-000000000002','a1b2c3d4-0002-0002-0002-000000000002', ARRAY['TEA'],  FALSE);

\echo '=== BLOCO 3: VINCULOS E PERMISSOES ==='

-- UUIDs links: 11c10001-XXXX-XXXX-XXXX-XXXXXXXXXXXX
INSERT INTO ng_core.ng_support_links (id, titular_id, apoio_id, invite_token_hash, invite_expires_at, status, accepted_at) VALUES
('11c10001-0001-0001-0001-000000000001',
 'a1b2c3d4-0001-0001-0001-000000000001',
 'b2c3d4e5-0004-0004-0004-000000000004',
 'sha256_invite_lucas_ana_aceito_01',
 NOW() - INTERVAL '10 days','ACTIVE', NOW() - INTERVAL '10 days'),

('11c10002-0001-0001-0001-000000000002',
 'a1b2c3d4-0002-0002-0002-000000000002',
 'b2c3d4e5-0005-0005-0005-000000000005',
 'sha256_invite_sofia_roberto_aceito',
 NOW() - INTERVAL '5 days','ACTIVE', NOW() - INTERVAL '5 days');

INSERT INTO ng_core.ng_support_links (id, titular_id, apoio_id, invite_token_hash, invite_expires_at, status, accepted_at, revoked_at) VALUES
('11c10003-0001-0001-0001-000000000003',
 'a1b2c3d4-0001-0001-0001-000000000001',
 'b2c3d4e5-0005-0005-0005-000000000005',
 'sha256_invite_lucas_roberto_revogado',
 NOW() - INTERVAL '20 days','REVOKED', NOW() - INTERVAL '18 days', NOW() - INTERVAL '3 days');

-- UUIDs permissoes: be110001-XXXX-XXXX-XXXX-XXXXXXXXXXXX
INSERT INTO ng_core.ng_link_permissions (id, link_id, module_name, granted_by, granted_at) VALUES
('be110001-0001-0001-0001-000000000001','11c10001-0001-0001-0001-000000000001','AGENDA',    'a1b2c3d4-0001-0001-0001-000000000001', NOW() - INTERVAL '10 days'),
('be110002-0001-0001-0001-000000000002','11c10001-0001-0001-0001-000000000001','FOCUS',     'a1b2c3d4-0001-0001-0001-000000000001', NOW() - INTERVAL '10 days'),
('be110003-0001-0001-0001-000000000003','11c10002-0001-0001-0001-000000000002','AGENDA',    'a1b2c3d4-0002-0002-0002-000000000002', NOW() - INTERVAL '5 days'),
('be110004-0001-0001-0001-000000000004','11c10002-0001-0001-0001-000000000002','EMOTIONAL', 'a1b2c3d4-0002-0002-0002-000000000002', NOW() - INTERVAL '5 days');

\echo '=== BLOCO 4: AGENDA ==='

-- UUIDs categorias: ca710001-XXXX-XXXX-XXXX-XXXXXXXXXXXX
INSERT INTO ng_core.ng_categories (id, user_id, name, color, icon) VALUES
('ca710001-0001-0001-0001-000000000001','a1b2c3d4-0001-0001-0001-000000000001','Trabalho','#4A90E2','briefcase'),
('ca710002-0001-0001-0001-000000000002','a1b2c3d4-0001-0001-0001-000000000001','Saude',   '#E24A4A','heart'),
('ca710003-0001-0001-0001-000000000003','a1b2c3d4-0001-0001-0001-000000000001','Pessoal', '#50C878','star');

-- UUIDs eventos: e0e10001-XXXX-XXXX-XXXX-XXXXXXXXXXXX
INSERT INTO ng_core.ng_events (id, owner_id, title, description, start_at, end_at, event_type, shared_with_support, category_id) VALUES
('e0e10001-0001-0001-0001-000000000001',
 'a1b2c3d4-0001-0001-0001-000000000001',
 'Consulta com psiquiatra',
 'Consulta mensal — Dr. Fernandes. Levar relatorio de humor.',
 NOW() + INTERVAL '3 days', NOW() + INTERVAL '3 days' + INTERVAL '1 hour',
 'APPOINTMENT', TRUE, 'ca710002-0001-0001-0001-000000000002');

INSERT INTO ng_core.ng_events (id, owner_id, title, description, start_at, end_at, event_type, rrule, shared_with_support, category_id) VALUES
('e0e10002-0001-0001-0001-000000000002',
 'a1b2c3d4-0001-0001-0001-000000000001',
 'Reuniao de equipe',
 'Stand-up semanal — segunda-feira as 9h',
 NOW() + INTERVAL '1 day', NOW() + INTERVAL '1 day' + INTERVAL '30 minutes',
 'APPOINTMENT','FREQ=WEEKLY;BYDAY=MO', FALSE, 'ca710001-0001-0001-0001-000000000001');

INSERT INTO ng_core.ng_events (id, owner_id, title, start_at, end_at, event_type, cancelled_at) VALUES
('e0e10003-0001-0001-0001-000000000003',
 'a1b2c3d4-0001-0001-0001-000000000001',
 'Happy hour cancelado',
 NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days' + INTERVAL '2 hours',
 'PERSONAL', NOW() - INTERVAL '3 days');

-- UUIDs tasks: ba5c0001-XXXX-XXXX-XXXX-XXXXXXXXXXXX
INSERT INTO ng_core.ng_tasks (id, owner_id, title, description, status, priority, due_date, shared_with_support, category_id) VALUES
('ba5c0001-0001-0001-0001-000000000001',
 'a1b2c3d4-0001-0001-0001-000000000001',
 'Entregar relatorio trimestral',
 'Relatorio Q4 — incluir metricas de foco e produtividade',
 'IN_PROGRESS','HIGH', CURRENT_DATE + 2, TRUE, 'ca710001-0001-0001-0001-000000000001');

INSERT INTO ng_core.ng_tasks (id, owner_id, title, status, priority, due_date, shared_with_support) VALUES
('ba5c0002-0001-0001-0001-000000000002',
 'a1b2c3d4-0001-0001-0001-000000000001',
 'Tomar medicacao diaria', 'PENDING','CRITICAL', CURRENT_DATE, FALSE);

INSERT INTO ng_core.ng_tasks (id, owner_id, title, status, priority, due_date) VALUES
('ba5c0003-0001-0001-0001-000000000003',
 'a1b2c3d4-0001-0001-0001-000000000001',
 'Organizar mesa de trabalho', 'COMPLETED','LOW', CURRENT_DATE - 1);

-- UUIDs subtasks: 5ab10001-XXXX-XXXX-XXXX-XXXXXXXXXXXX
INSERT INTO ng_core.ng_subtasks (id, task_id, title, done, position) VALUES
('5ab10001-0001-0001-0001-000000000001','ba5c0001-0001-0001-0001-000000000001','Coletar dados de outubro',      TRUE,  1),
('5ab10002-0001-0001-0001-000000000002','ba5c0001-0001-0001-0001-000000000001','Coletar dados de novembro',     FALSE, 2),
('5ab10003-0001-0001-0001-000000000003','ba5c0001-0001-0001-0001-000000000001','Montar slides de apresentacao', FALSE, 3),
('5ab10004-0001-0001-0001-000000000004','ba5c0001-0001-0001-0001-000000000001','Revisar com lider tecnico',     FALSE, 4);

-- UUIDs reminders: ae010001-XXXX-XXXX-XXXX-XXXXXXXXXXXX
INSERT INTO ng_core.ng_reminders (id, task_id, minutes_before, persistent, repeat_interval_min, max_repetitions, current_repetitions, status, next_fire_at) VALUES
('ae010001-0001-0001-0001-000000000001','ba5c0002-0001-0001-0001-000000000002',
 0, TRUE, 30, 5, 2, 'SCHEDULED', NOW() + INTERVAL '30 minutes');

INSERT INTO ng_core.ng_reminders (id, event_id, minutes_before, persistent, status, next_fire_at) VALUES
('ae010002-0001-0001-0001-000000000002','e0e10001-0001-0001-0001-000000000001',
 60, FALSE, 'SCHEDULED', NOW() + INTERVAL '3 days' - INTERVAL '1 hour');

\echo '=== BLOCO 5: FOCO E EMOCIONAL ==='

-- UUIDs focus: f0c5e001-XXXX-XXXX-XXXX-XXXXXXXXXXXX
INSERT INTO ng_core.ng_focus_sessions (id, user_id, task_id, planned_minutes, break_minutes, status, started_at, ended_at, effective_seconds) VALUES
('f0c5e001-0001-0001-0001-000000000001',
 'a1b2c3d4-0001-0001-0001-000000000001',
 'ba5c0001-0001-0001-0001-000000000001',
 25, 5,'COMPLETED',
 NOW() - INTERVAL '2 hours',
 NOW() - INTERVAL '2 hours' + INTERVAL '28 minutes',
 1500);

-- UUIDs pauses: fa0ce001-XXXX-XXXX-XXXX-XXXXXXXXXXXX
INSERT INTO ng_core.ng_focus_pauses (id, session_id, paused_at, resumed_at) VALUES
('fa0ce001-0001-0001-0001-000000000001',
 'f0c5e001-0001-0001-0001-000000000001',
 NOW() - INTERVAL '2 hours' + INTERVAL '15 minutes',
 NOW() - INTERVAL '2 hours' + INTERVAL '18 minutes');

-- UUIDs mood: b00dc001-XXXX-XXXX-XXXX-XXXXXXXXXXXX
INSERT INTO ng_sensitive.ng_mood_checkins (id, user_id, mood_score, energy_level, checkin_date, checkin_time) VALUES
('b00dc001-0001-0001-0001-000000000001','a1b2c3d4-0001-0001-0001-000000000001', 7, 6, CURRENT_DATE - 2,'08:30'),
('b00dc002-0001-0001-0001-000000000002','a1b2c3d4-0001-0001-0001-000000000001', 5, 4, CURRENT_DATE - 1,'09:00'),
('b00dc003-0001-0001-0001-000000000003','a1b2c3d4-0001-0001-0001-000000000001', 8, 7, CURRENT_DATE,    '08:45');

\echo '=== BLOCO 6: LOGS IDENTITY E VAULT ==='

-- UUIDs audit_logs: a10b0001-XXXX-XXXX-XXXX-XXXXXXXXXXXX
INSERT INTO ng_identity.ng_audit_logs (id, user_id, action, resource_type, resource_id, ip_address, metadata) VALUES
('a10b0001-0001-0001-0001-000000000001','a1b2c3d4-0001-0001-0001-000000000001','USER_LOGIN',                  'User',        'a1b2c3d4-0001-0001-0001-000000000001','177.42.100.55','{"method":"password","success":true}'),
('a10b0002-0001-0001-0001-000000000002','a1b2c3d4-0001-0001-0001-000000000001','LINK_CREATED',                'SupportLink', '11c10001-0001-0001-0001-000000000001','177.42.100.55','{"apoio_email":"ana.lima@neurogen.demo"}'),
('a10b0003-0001-0001-0001-000000000003','a1b2c3d4-0001-0001-0001-000000000001','LINK_REVOKED',                'SupportLink', '11c10003-0001-0001-0001-000000000003','177.42.100.55','{"reason":"user_request"}'),
('a10b0004-0001-0001-0001-000000000004','b2c3d4e5-0004-0004-0004-000000000004','VAULT_ACCESS_DENIED_APOIO',   'Vault',       NULL,                                  '192.168.1.10', '{"attempted_path":"/api/v1/vault/entries","role":"APOIO"}'),
('a10b0005-0001-0001-0001-000000000005',NULL,                                  'LOGIN_FAILED_ACCOUNT_SUSPENDED','User',      'c3d4e5f6-0006-0006-0006-000000000006','10.0.0.99',    '{"attempts":8,"reason":"account_suspended"}');

INSERT INTO ng_vault.ng_vault_auth_logs (id, user_id, action, ip_address, success, failure_reason, attempted_at) VALUES
('d0a10001-0001-0001-0001-000000000001','a1b2c3d4-0001-0001-0001-000000000001','OPEN',                '177.42.100.55', TRUE,  NULL,                                        NOW() - INTERVAL '1 hour'),
('d0a10002-0001-0001-0001-000000000002','b2c3d4e5-0004-0004-0004-000000000004','ACCESS_DENIED_APOIO', '192.168.1.10',  FALSE,'APOIO role structurally denied (RF-08.5)',   NOW() - INTERVAL '45 minutes'),
('d0a10003-0001-0001-0001-000000000003','b2c3d4e5-0005-0005-0005-000000000005','ACCESS_DENIED_APOIO', '200.100.50.20', FALSE,'APOIO role structurally denied (RF-08.5)',   NOW() - INTERVAL '30 minutes'),
('d0a10004-0001-0001-0001-000000000004','a1b2c3d4-0001-0001-0001-000000000001','CLOSE',               '177.42.100.55', TRUE,  NULL,                                        NOW() - INTERVAL '20 minutes');

INSERT INTO ng_audit.ng_system_logs (logged_at, level, logger_name, message, user_id, action, resource_type, resource_id, ip_address, http_method, http_status, duration_ms, metadata) VALUES
(NOW()-INTERVAL '3 hours',          'INFO','AuditService','Acao de dominio: USER_LOGIN',               'a1b2c3d4-0001-0001-0001-000000000001','USER_LOGIN',                  'User',        'a1b2c3d4-0001-0001-0001-000000000001','177.42.100.55','POST',200,145, '{"method":"password","success":true}'),
(NOW()-INTERVAL '170 minutes',      'INFO','AuditService','Acao de dominio: LINK_CREATED',             'a1b2c3d4-0001-0001-0001-000000000001','LINK_CREATED',                'SupportLink', '11c10001-0001-0001-0001-000000000001','177.42.100.55','POST',201,320, '{"apoio_email":"ana.lima@neurogen.demo","ttl_hours":72}'),
(NOW()-INTERVAL '90 minutes',       'WARN','AuditService','[SEC-FAIL] VAULT_ACCESS_DENIED_APOIO',      'b2c3d4e5-0004-0004-0004-000000000004','VAULT_ACCESS_DENIED_APOIO',   'Vault',       NULL,                                '192.168.1.10', 'GET', 403,12,  '{"role":"APOIO","attempted_path":"/api/v1/vault/entries"}'),
(NOW()-INTERVAL '60 minutes',       'INFO','AuditService','Acao de dominio: VAULT_OPENED',             'a1b2c3d4-0001-0001-0001-000000000001','VAULT_OPENED',                'Vault',       NULL,                                '177.42.100.55','POST',200,850, '{"session_ttl_seconds":300}'),
(NOW()-INTERVAL '45 minutes',       'WARN','AuditService','[SEC-FAIL] VAULT_ACCESS_DENIED_APOIO',      'b2c3d4e5-0005-0005-0005-000000000005','VAULT_ACCESS_DENIED_APOIO',   'Vault',       NULL,                                '200.100.50.20','POST',403,8,   '{"role":"APOIO","attempted_path":"/api/v1/vault/open"}'),
(NOW()-INTERVAL '30 minutes',       'WARN','AuditService','[SEC-FAIL] USER_LOGIN conta suspensa',      'c3d4e5f6-0006-0006-0006-000000000006','USER_LOGIN',                  'User',        'c3d4e5f6-0006-0006-0006-000000000006','10.0.0.99',   'POST',401,55,  '{"reason":"ACCOUNT_SUSPENDED","attempts_blocked":true}'),
(NOW()-INTERVAL '10 minutes',       'WARN','AuditService','[SEC-FAIL] MINOR_REGISTRATION_BLOCKED',     NULL,                                  'MINOR_REGISTRATION_BLOCKED',  'User',        NULL,                                '203.45.67.89', 'POST',422,30,  '{"declared_birth_date":"2010-05-14","age_calculated":16,"minimum_age":18,"rule":"RF-01.0"}');

\echo '=== BLOCO 7: UPDATES E DELETES (geram audit trail automatico) ==='

UPDATE ng_core.ng_tasks SET status = 'IN_PROGRESS', updated_at = NOW()
WHERE id = 'ba5c0002-0001-0001-0001-000000000002';

UPDATE ng_core.ng_subtasks SET done = TRUE, completed_at = NOW()
WHERE id = '5ab10002-0001-0001-0001-000000000002';

UPDATE ng_core.ng_user_profiles
SET sensory_preferences = '{"reduce_animations":true,"high_contrast":true,"font_size":"large"}',
    updated_at = NOW()
WHERE id = 'a1b2c3d4-0001-0001-0001-000000000001';

UPDATE ng_identity.ng_users
SET status = 'SUSPENDED', updated_at = NOW()
WHERE id = 'c3d4e5f6-0007-0007-0007-000000000007';

UPDATE ng_core.ng_support_links
SET status = 'REVOKED', revoked_at = NOW(), updated_at = NOW()
WHERE id = '11c10003-0001-0001-0001-000000000003';

DELETE FROM ng_core.ng_subtasks WHERE id = '5ab10004-0001-0001-0001-000000000004';

DELETE FROM ng_core.ng_events WHERE id = 'e0e10003-0001-0001-0001-000000000003';

\echo '=== RESUMO FINAL ==='

SELECT tabela, contagem FROM (
  SELECT 1 AS ord, 'ng_identity.ng_users'          AS tabela, COUNT(*)::text AS contagem FROM ng_identity.ng_users
  UNION ALL SELECT 2,'ng_identity.ng_sessions',        COUNT(*)::text FROM ng_identity.ng_sessions
  UNION ALL SELECT 3,'ng_identity.ng_consent_records', COUNT(*)::text FROM ng_identity.ng_consent_records
  UNION ALL SELECT 4,'ng_identity.ng_audit_logs',      COUNT(*)::text FROM ng_identity.ng_audit_logs
  UNION ALL SELECT 5,'ng_core.ng_support_links',       COUNT(*)::text FROM ng_core.ng_support_links
  UNION ALL SELECT 6,'ng_core.ng_link_permissions',    COUNT(*)::text FROM ng_core.ng_link_permissions
  UNION ALL SELECT 7,'ng_core.ng_user_profiles',       COUNT(*)::text FROM ng_core.ng_user_profiles
  UNION ALL SELECT 8,'ng_core.ng_categories',          COUNT(*)::text FROM ng_core.ng_categories
  UNION ALL SELECT 9,'ng_core.ng_events',              COUNT(*)::text FROM ng_core.ng_events
  UNION ALL SELECT 10,'ng_core.ng_tasks',              COUNT(*)::text FROM ng_core.ng_tasks
  UNION ALL SELECT 11,'ng_core.ng_subtasks',           COUNT(*)::text FROM ng_core.ng_subtasks
  UNION ALL SELECT 12,'ng_core.ng_reminders',          COUNT(*)::text FROM ng_core.ng_reminders
  UNION ALL SELECT 13,'ng_core.ng_focus_sessions',     COUNT(*)::text FROM ng_core.ng_focus_sessions
  UNION ALL SELECT 14,'ng_sensitive.ng_mood_checkins', COUNT(*)::text FROM ng_sensitive.ng_mood_checkins
  UNION ALL SELECT 15,'ng_audit.ng_db_audit',          COUNT(*)::text FROM ng_audit.ng_db_audit
  UNION ALL SELECT 16,'ng_audit.ng_system_logs',       COUNT(*)::text FROM ng_audit.ng_system_logs
  UNION ALL SELECT 17,'ng_vault.ng_vault_auth_logs',   COUNT(*)::text FROM ng_vault.ng_vault_auth_logs
) t ORDER BY ord;
