-- RF-01.1: CPF e data de nascimento no cadastro
ALTER TABLE ng_identity.ng_users
    ADD COLUMN cpf        VARCHAR(11)  UNIQUE,
    ADD COLUMN birth_date DATE;

-- RF-01.9: flag que distingue segredo TOTP pendente (gerado, não confirmado) de ativo
ALTER TABLE ng_identity.ng_totp_secrets
    ADD COLUMN setup_shown BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN confirmed   BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill: registros existentes já passaram pelo fluxo anterior (totp_enabled = true na tabela de
-- usuários), portanto são considerados confirmados e exibidos. Sem isso, usuários com 2FA ativo
-- teriam o factor silenciosamente desativado após a migration — bypass de autenticação.
UPDATE ng_identity.ng_totp_secrets ts
SET confirmed   = TRUE,
    setup_shown = TRUE
FROM ng_identity.ng_users u
WHERE u.id = ts.user_id
  AND u.totp_enabled = TRUE;
