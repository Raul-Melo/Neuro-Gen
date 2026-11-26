# Guia de Validação do Banco de Dados — Neuro-Gen

> **Pré-requisito:** Docker rodando e container `neurogen-postgres` no ar.  
> Para verificar: `docker compose ps`

---

## Sumário

1. [Como conectar ao banco](#1-como-conectar-ao-banco)
2. [Dados de demonstração já inseridos](#2-dados-de-demonstração-já-inseridos)
3. [Validar inserção e audit trail automático](#3-validar-inserção-e-audit-trail-automático)
4. [Validar UPDATE com old_data / new_data](#4-validar-update-com-old_data--new_data)
5. [Validar DELETE auditado](#5-validar-delete-auditado)
6. [Validar conta SUSPENDED bloqueia acesso](#6-validar-conta-suspended-bloqueia-acesso)
7. [Validar menor de idade bloqueado](#7-validar-menor-de-idade-bloqueado)
8. [Validar APOIO negado no Vault](#8-validar-apoio-negado-no-vault)
9. [Verificar imutabilidade do audit](#9-verificar-imutabilidade-do-audit)
10. [Consultas forenses completas](#10-consultas-forenses-completas)
11. [Reexecutar os dados de demo](#11-reexecutar-os-dados-de-demo)

---

## 1. Como conectar ao banco

### Via psql (linha de comando)

```powershell
# Acessa o psql diretamente dentro do container
docker exec -it neurogen-postgres psql -U neurogen -d neurogen
```

Você verá o prompt `neurogen=#`. Para sair: `\q`

### Comandos úteis dentro do psql

```sql
\dn          -- lista todos os schemas
\dt ng_identity.*   -- lista tabelas do schema ng_identity
\dt ng_core.*       -- lista tabelas do schema ng_core
\dt ng_audit.*      -- lista tabelas de auditoria
\d ng_audit.ng_db_audit   -- descreve a estrutura da tabela
\x                  -- modo expandido (melhor para linhas largas)
\x auto             -- muda automaticamente conforme a largura
```

### Via pgAdmin (interface web)

1. Abra http://localhost:5050 no navegador
2. Login: `admin@admin.com` / `admin`
3. Clique em **Add New Server**:
   - **Name:** Neuro-Gen Local
   - **Host:** `postgres` (dentro da rede Docker) **ou** `localhost` (da máquina host)
   - **Port:** `5432` (dentro do Docker) / `5433` (da máquina host)
   - **Database:** `neurogen`
   - **Username:** `neurogen`
   - **Password:** `secret`
4. Expanda a árvore: Servers → Neuro-Gen Local → Databases → neurogen → Schemas

> **Dica:** Para executar queries no pgAdmin: clique com botão direito em qualquer schema → **Query Tool**

### Via DBeaver / DataGrip / outro cliente

Configuração de conexão:

| Campo | Valor |
|---|---|
| Host | `localhost` |
| Port | `5433` |
| Database | `neurogen` |
| Username | `neurogen` |
| Password | `secret` |

---

## 2. Dados de demonstração já inseridos

Execute o script de demo para popular o banco (pode ser rodado quantas vezes quiser — faz cleanup antes):

```powershell
docker exec -i neurogen-postgres psql -U neurogen -d neurogen < scripts/demo-data.sql
```

**Usuários criados:**

| Email | Role | Status | Propósito do teste |
|---|---|---|---|
| `lucas.oliveira@neurogen.demo` | TITULAR | ACTIVE | Usuário principal — tem tarefas, eventos, sessões de foco |
| `sofia.martins@neurogen.demo` | TITULAR | ACTIVE | Titular com vínculo APOIO ativo |
| `pedro.alves@neurogen.demo` | TITULAR | ACTIVE | TOTP habilitado |
| `ana.lima@neurogen.demo` | APOIO | ACTIVE | Vinculada a Lucas — testa negação do Vault |
| `roberto.souza@neurogen.demo` | APOIO | ACTIVE | Vinculado a Sofia |
| `conta.suspensa@neurogen.demo` | TITULAR | **SUSPENDED** | Testa bloqueio de conta suspensa |
| `conta.pendente@neurogen.demo` | TITULAR | **PENDING_VERIFICATION** | Suspensa no Bloco 7 (UPDATE) |

**IDs importantes para usar nas queries:**

```sql
-- Lucas (titular ativo)
'a1b2c3d4-0001-0001-0001-000000000001'

-- Conta suspensa
'c3d4e5f6-0006-0006-0006-000000000006'

-- Ana Lima (APOIO que tenta acessar o Vault)
'b2c3d4e5-0004-0004-0004-000000000004'
```

---

## 3. Validar inserção e audit trail automático

### 3.1 — Confirmar que os dados existem

```sql
-- Contar usuários de demo
SELECT id, email, role, status, email_verified, created_at
FROM ng_identity.ng_users
WHERE email LIKE '%@neurogen.demo'
ORDER BY created_at;
```

**Resultado esperado:** 7 linhas com os usuários de demo.

### 3.2 — Ver os registros de INSERT no ng_db_audit

```sql
-- INSERTs na tabela ng_users capturados pelos triggers
SELECT
  occurred_at,
  operation,
  schema_name,
  table_name,
  record_id,
  new_data,
  db_user
FROM ng_audit.ng_db_audit
WHERE table_name = 'ng_users'
  AND operation = 'INSERT'
ORDER BY occurred_at DESC
LIMIT 10;
```

**Resultado esperado:** Linhas com `operation = 'INSERT'`, `new_data` com os dados do usuário. Note que **`password_hash` não aparece em `new_data`** — foi removido pelo trigger `fn_capture_dml_redacted`.

### 3.3 — Confirmar que password_hash foi redacted

```sql
-- Verifica que password_hash NÃO aparece no audit
SELECT
  record_id,
  new_data ? 'password_hash' AS tem_password_hash,
  new_data ? 'email'         AS tem_email,
  new_data ? 'role'          AS tem_role
FROM ng_audit.ng_db_audit
WHERE table_name = 'ng_users'
  AND operation = 'INSERT'
LIMIT 5;
```

**Resultado esperado:** `tem_password_hash = false`, `tem_email = true`, `tem_role = true`.

### 3.4 — Inserir um novo usuário e verificar o audit imediatamente

```sql
-- Insere um usuário
INSERT INTO ng_identity.ng_users (id, email, password_hash, role, status)
VALUES (
  gen_random_uuid(),
  'teste.manual@neurogen.demo',
  'argon2id:teste:hash',
  'TITULAR', 'ACTIVE'
);

-- Vê o registro de audit gerado automaticamente (últimos 3)
SELECT occurred_at, operation, table_name, record_id, new_data
FROM ng_audit.ng_db_audit
WHERE table_name = 'ng_users'
ORDER BY occurred_at DESC
LIMIT 3;
```

---

## 4. Validar UPDATE com old_data / new_data

O script de demo já executou 5 UPDATEs. Veja os resultados:

```sql
-- Ver todos os UPDATEs capturados com before/after
SELECT
  occurred_at,
  table_name,
  record_id,
  old_data,
  new_data
FROM ng_audit.ng_db_audit
WHERE operation = 'UPDATE'
ORDER BY occurred_at DESC
LIMIT 10;
```

### 4.1 — Ver o UPDATE específico de suspensão de conta

```sql
-- Conta pendente foi suspensa no Bloco 7
SELECT
  occurred_at,
  operation,
  record_id,
  old_data->>'status' AS status_antes,
  new_data->>'status' AS status_depois
FROM ng_audit.ng_db_audit
WHERE table_name = 'ng_users'
  AND operation = 'UPDATE'
  AND record_id = 'c3d4e5f6-0007-0007-0007-000000000007';
```

**Resultado esperado:** `status_antes = PENDING_VERIFICATION`, `status_depois = SUSPENDED`.

### 4.2 — Fazer um UPDATE agora e ver o audit

```sql
-- Altera o status da tarefa de Lucas
UPDATE ng_core.ng_tasks
SET status = 'COMPLETED', updated_at = NOW()
WHERE id = 'ba5c0001-0001-0001-0001-000000000001';

-- Ver o audit do UPDATE
SELECT
  occurred_at,
  old_data->>'status' AS status_antes,
  new_data->>'status' AS status_depois,
  old_data->>'title'  AS titulo
FROM ng_audit.ng_db_audit
WHERE table_name = 'ng_tasks'
  AND operation = 'UPDATE'
  AND record_id = 'ba5c0001-0001-0001-0001-000000000001'
ORDER BY occurred_at DESC
LIMIT 1;
```

---

## 5. Validar DELETE auditado

O script de demo executou 2 DELETEs. Veja que o `old_data` preserva o snapshot do registro deletado:

```sql
-- DELETEs capturados — old_data contém o dado que foi removido
SELECT
  occurred_at,
  table_name,
  record_id,
  old_data,
  new_data   -- NULL para DELETE
FROM ng_audit.ng_db_audit
WHERE operation = 'DELETE'
ORDER BY occurred_at DESC
LIMIT 10;
```

**Resultado esperado:** `new_data = NULL`, `old_data` com o conteúdo completo da linha antes de ser deletada.

### 5.1 — Deletar e recuperar via audit

```sql
-- Deleta uma categoria
DELETE FROM ng_core.ng_categories
WHERE id = 'ca710003-0001-0001-0001-000000000003';

-- A categoria sumiu da tabela, mas o audit preservou tudo
SELECT
  'REMOVIDA DA TABELA: ' || (SELECT COUNT(*) FROM ng_core.ng_categories WHERE id = 'ca710003-0001-0001-0001-000000000003')::text || ' linhas' AS na_tabela,
  'NO AUDIT: ' || (SELECT COUNT(*) FROM ng_audit.ng_db_audit WHERE record_id = 'ca710003-0001-0001-0001-000000000003')::text || ' registros' AS no_audit;

-- Recuperar os dados do registro deletado
SELECT old_data FROM ng_audit.ng_db_audit
WHERE record_id = 'ca710003-0001-0001-0001-000000000003'
  AND operation = 'DELETE';
```

---

## 6. Validar conta SUSPENDED bloqueia acesso

### 6.1 — Verificar o status da conta suspensa no banco

```sql
SELECT id, email, role, status, email_verified, updated_at
FROM ng_identity.ng_users
WHERE email = 'conta.suspensa@neurogen.demo';
```

**Resultado esperado:** `status = SUSPENDED`.

### 6.2 — Ver o log de bloqueio de acesso

```sql
-- Log da tentativa de login da conta suspensa
SELECT logged_at, level, action, user_id, ip_address, http_status, metadata
FROM ng_audit.ng_system_logs
WHERE action = 'USER_LOGIN'
  AND user_id = 'c3d4e5f6-0006-0006-0006-000000000006'
ORDER BY logged_at DESC;
```

**Resultado esperado:** `level = WARN`, `http_status = 401`, `metadata` com `reason = ACCOUNT_SUSPENDED`.

### 6.3 — Testar via API (com aplicação rodando)

```powershell
# Tentativa de login com conta suspensa — deve retornar 401
curl -s -X POST http://localhost:8080/api/v1/auth/login `
  -H "Content-Type: application/json" `
  -d '{"email":"conta.suspensa@neurogen.demo","password":"qualquer"}' | ConvertFrom-Json
```

**Resultado esperado:** HTTP 401 com mensagem de conta suspensa.

---

## 7. Validar menor de idade bloqueado

A regra RF-01.0 bloqueia cadastros de pessoas com menos de 18 anos.

### 7.1 — Ver o log do bloqueio (inserido como demo)

```sql
SELECT logged_at, level, action, ip_address, metadata
FROM ng_audit.ng_system_logs
WHERE action = 'MINOR_REGISTRATION_BLOCKED'
ORDER BY logged_at DESC;
```

**Resultado esperado:** `level = WARN`, `metadata` com `age_calculated = 16`, `minimum_age = 18`, `rule = RF-01.0`.

### 7.2 — Testar via API (com aplicação rodando)

```powershell
# Tentativa de registro com data de nascimento que resulta em < 18 anos
curl -s -X POST http://localhost:8080/api/v1/auth/register `
  -H "Content-Type: application/json" `
  -d '{
    "email": "menor@neurogen.demo",
    "password": "Senha@2025",
    "birthDate": "2010-05-14"
  }' | ConvertFrom-Json
```

**Resultado esperado:** HTTP 422 (Unprocessable Entity) ou 400 com mensagem de restrição de idade.

---

## 8. Validar APOIO negado no Vault

Contas com role `APOIO` são estruturalmente bloqueadas do Vault (RF-08.5). O bloqueio ocorre na camada de segurança (Spring Security filter chain 2), antes mesmo de chegar ao use case.

### 8.1 — Ver os logs de negação no banco

```sql
-- Tentativas de APOIO acessando o Vault (ng_vault_auth_logs)
SELECT attempted_at, user_id, action, ip_address, success, failure_reason
FROM ng_vault.ng_vault_auth_logs
WHERE success = FALSE
ORDER BY attempted_at DESC;
```

**Resultado esperado:** Registros com `action = ACCESS_DENIED_APOIO`, `success = false`, `failure_reason = APOIO role structurally denied (RF-08.5)`.

### 8.2 — Ver os logs de segurança no sistema

```sql
-- Sistema_logs: negações do Vault por APOIO
SELECT logged_at, level, user_id, ip_address, http_status, metadata
FROM ng_audit.ng_system_logs
WHERE action = 'VAULT_ACCESS_DENIED_APOIO'
ORDER BY logged_at DESC;
```

**Resultado esperado:** `level = WARN`, `http_status = 403`, `metadata` com `role = APOIO`.

### 8.3 — Ver o audit do ng_db_audit para o vault_auth_logs

```sql
-- Os INSERTs nos vault_auth_logs também são auditados via trigger
SELECT occurred_at, operation, record_id, new_data->>'action', new_data->>'success'
FROM ng_audit.ng_db_audit
WHERE table_name = 'ng_vault_auth_logs'
ORDER BY occurred_at DESC;
```

### 8.4 — Testar via API (com aplicação rodando)

```powershell
# Primeiro, faz login com a conta APOIO para obter o token
$login = curl -s -X POST http://localhost:8080/api/v1/auth/login `
  -H "Content-Type: application/json" `
  -d '{"email":"ana.lima@neurogen.demo","password":"demo"}' | ConvertFrom-Json

$token = $login.accessToken

# Tenta acessar o Vault — deve retornar 403
curl -s -X GET http://localhost:8080/api/v1/vault/entries `
  -H "Authorization: Bearer $token"
```

**Resultado esperado:** HTTP 403 Forbidden.

---

## 9. Verificar imutabilidade do audit

Tente forçar uma mutação nas tabelas de auditoria — deve falhar com `SQLSTATE 55000`:

```sql
-- Tentativa de UPDATE na ng_db_audit (DEVE FALHAR)
UPDATE ng_audit.ng_db_audit
SET operation = 'FAKE'
WHERE id = (SELECT id FROM ng_audit.ng_db_audit LIMIT 1);
```

**Resultado esperado:**

```
ERROR:  55000: Operação proibida: a tabela de auditoria "ng_db_audit" é imutável.
DETAIL:  UPDATE e DELETE são bloqueados por trigger para garantir integridade forense.
```

```sql
-- Tentativa de DELETE na ng_system_logs (DEVE FALHAR)
DELETE FROM ng_audit.ng_system_logs
WHERE id = (SELECT id FROM ng_audit.ng_system_logs LIMIT 1);
```

**Resultado esperado:** Mesmo erro — `55000`.

---

## 10. Consultas forenses completas

### Histórico completo de um usuário

```sql
-- Todas as ações de Lucas no banco (via triggers)
SELECT occurred_at, operation, schema_name, table_name, record_id
FROM ng_audit.ng_db_audit
WHERE app_user_id = 'a1b2c3d4-0001-0001-0001-000000000001'
ORDER BY occurred_at;

-- Todas as ações de Lucas no sistema (via AuditService)
SELECT logged_at, level, action, resource_type, resource_id, ip_address, http_status
FROM ng_audit.ng_system_logs
WHERE user_id = 'a1b2c3d4-0001-0001-0001-000000000001'
ORDER BY logged_at;
```

### Volume de operações por tabela

```sql
SELECT table_name, operation, COUNT(*) AS total
FROM ng_audit.ng_db_audit
GROUP BY table_name, operation
ORDER BY total DESC;
```

### Eventos de segurança com falha (nível WARN ou acima)

```sql
SELECT logged_at, level, action, user_id, ip_address,
       http_status, metadata->>'reason' AS motivo
FROM ng_audit.ng_system_logs
WHERE level IN ('WARN','ERROR','FATAL')
  AND action IS NOT NULL
ORDER BY logged_at DESC;
```

### Rastrear uma transação específica (todas as tabelas afetadas)

```sql
-- Pega o tx_id da última transação
SELECT DISTINCT tx_id FROM ng_audit.ng_db_audit ORDER BY tx_id DESC LIMIT 5;

-- Vê tudo que aconteceu em uma transação
SELECT occurred_at, operation, schema_name || '.' || table_name AS tabela, record_id
FROM ng_audit.ng_db_audit
WHERE tx_id = <cole_o_tx_id_aqui>
ORDER BY occurred_at;
```

### Comparação before/after de um registro específico

```sql
-- Histórico completo da tarefa de relatório de Lucas
SELECT
  occurred_at,
  operation,
  old_data->>'status' AS status_antes,
  new_data->>'status' AS status_depois,
  old_data->>'priority' AS prio_antes,
  new_data->>'priority' AS prio_depois
FROM ng_audit.ng_db_audit
WHERE table_name = 'ng_tasks'
  AND record_id = 'ba5c0001-0001-0001-0001-000000000001'
ORDER BY occurred_at;
```

### Contagem total de operações

```sql
SELECT
  (SELECT COUNT(*) FROM ng_audit.ng_db_audit)   AS total_db_audit,
  (SELECT COUNT(*) FROM ng_audit.ng_system_logs) AS total_system_logs,
  (SELECT COUNT(*) FROM ng_vault.ng_vault_auth_logs) AS total_vault_logs;
```

---

## 11. Reexecutar os dados de demo

O script é **idempotente**: apaga os dados de demo antes de reinserir.

```powershell
# Windows PowerShell — reexecuta tudo do zero
docker exec -i neurogen-postgres psql -U neurogen -d neurogen < scripts/demo-data.sql

# Para ver só o resumo de contagem final:
docker exec -it neurogen-postgres psql -U neurogen -d neurogen -c "
SELECT tabela, contagem FROM (
  SELECT 1 AS ord, 'ng_identity.ng_users'          AS tabela, COUNT(*)::text AS contagem FROM ng_identity.ng_users
  UNION ALL SELECT 2,'ng_identity.ng_sessions',        COUNT(*)::text FROM ng_identity.ng_sessions
  UNION ALL SELECT 3,'ng_core.ng_tasks',               COUNT(*)::text FROM ng_core.ng_tasks
  UNION ALL SELECT 4,'ng_core.ng_events',              COUNT(*)::text FROM ng_core.ng_events
  UNION ALL SELECT 5,'ng_audit.ng_db_audit',           COUNT(*)::text FROM ng_audit.ng_db_audit
  UNION ALL SELECT 6,'ng_audit.ng_system_logs',        COUNT(*)::text FROM ng_audit.ng_system_logs
  UNION ALL SELECT 7,'ng_vault.ng_vault_auth_logs',    COUNT(*)::text FROM ng_vault.ng_vault_auth_logs
) t ORDER BY ord;
"
```

---

## Checklist de validação completa

| # | Cenário | Query | Resultado esperado |
|---|---|---|---|
| 1 | INSERTs aparecem no audit | Seção 3.2 | 7 usuários em `ng_db_audit` com `operation = INSERT` |
| 2 | `password_hash` redacted | Seção 3.3 | `new_data ? 'password_hash' = false` |
| 3 | UPDATE registra before/after | Seção 4.1 | `old_data` ≠ `new_data`, ambos com snapshot |
| 4 | DELETE preserva old_data | Seção 5 | `old_data` com dados, `new_data = NULL` |
| 5 | Conta SUSPENDED no banco | Seção 6.1 | `status = SUSPENDED` |
| 6 | Log de bloqueio de suspensão | Seção 6.2 | `WARN, http_status = 401` |
| 7 | Menor bloqueado no log | Seção 7.1 | `MINOR_REGISTRATION_BLOCKED, age = 16` |
| 8 | APOIO negado no Vault | Seção 8.1 | `success = false, ACCESS_DENIED_APOIO` |
| 9 | Audit imutável | Seção 9 | `ERROR 55000` ao tentar UPDATE/DELETE |
| 10 | Trigger cobre todas as tabelas | Seção 10 | Volume de operações por tabela |
