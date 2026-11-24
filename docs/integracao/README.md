# Neuro-Gen — Manual de Integração e Operação

> **Versão:** v2  |  **Stack:** Spring Boot 3.3.5 · Java 21 · PostgreSQL 16

---

## Sumário

1. [Pré-requisitos](#pré-requisitos)
2. [Iniciar com Docker (modo dev)](#iniciar-com-docker-modo-dev)
3. [Iniciar com Docker (modo completo)](#iniciar-com-docker-modo-completo)
4. [Iniciar localmente (sem Docker)](#iniciar-localmente-sem-docker)
5. [Executar os testes](#executar-os-testes)
6. [Variáveis de ambiente](#variáveis-de-ambiente)
7. [CORS — integração com frontend](#cors--integração-com-frontend)
8. [Endpoints por módulo](#endpoints-por-módulo)
9. [Fluxo de autenticação](#fluxo-de-autenticação)
10. [Auditoria](#auditoria)

---

## Pré-requisitos

| Ferramenta | Versão mínima | Observação |
|---|---|---|
| Docker Desktop | 4.x | Deve estar rodando antes de qualquer `docker compose` |
| Java (JDK) | 21 | Só necessário para rodar sem Docker |
| Maven | 3.9 (wrapper incluído) | Use `.\mvnw` (Windows) ou `./mvnw` (Linux/Mac) |

> **Conflito de porta no Windows:** se você tiver o PostgreSQL instalado localmente (serviço `postgresql-x64-*`), ele já ocupa a porta 5432. O Docker do Neuro-Gen usa a porta **5433** justamente para evitar esse conflito.

---

## Iniciar com Docker (modo dev)

Este modo sobe **apenas o banco** (PostgreSQL + pgAdmin). A aplicação roda na sua máquina com `.\mvnw spring-boot:run`, o que permite hot-reload e debug.

```powershell
# 1. Sobe o postgres e o pgAdmin
docker compose up -d

# 2. Verifica o status (aguarda "healthy")
docker compose ps

# 3. Roda a aplicação localmente (Flyway aplica as migrations automaticamente)
.\mvnw spring-boot:run
```

Serviços disponíveis após a inicialização:

| Serviço | URL | Observação |
|---|---|---|
| API | http://localhost:8080 | Backend Spring Boot |
| Swagger UI | http://localhost:8080/swagger-ui.html | Documentação interativa |
| pgAdmin | http://localhost:5050 | Interface do banco |
| PostgreSQL | localhost:**5433** | Porta 5433 (5432 pode estar ocupada) |

**Login pgAdmin:** `admin@admin.com` / valor de `PGADMIN_PASSWORD` (padrão `admin` em dev)

**Conexão no pgAdmin (adicionar servidor):**
- Host: `postgres` (dentro da rede Docker) ou `localhost` (da máquina host)
- Porta: `5432` (dentro do Docker) / `5433` (da máquina host)
- Banco: `neurogen`
- Usuário: `neurogen`
- Senha: `secret`

---

## Iniciar com Docker (modo completo)

Este modo constrói a imagem Docker da aplicação e sobe tudo junto. Ideal para validar o build de produção localmente.

```powershell
# 1. Copie e preencha o .env
cp .env.example .env
# Edite .env e defina SENSITIVE_DATA_KEY com 64 chars hex

# 2. Sobe tudo (build + postgres + pgadmin + app)
docker compose -f docker-compose.full.yml --env-file .env up --build -d

# 3. Acompanhe os logs da aplicação
docker compose -f docker-compose.full.yml logs -f app

# 4. Para parar tudo
docker compose -f docker-compose.full.yml down
```

> **SENSITIVE_DATA_KEY** é obrigatória no modo completo. Gere uma com:
> ```powershell
> # Windows PowerShell:
> -join ((1..32) | ForEach-Object { '{0:x2}' -f (Get-Random -Max 256) })
> # Linux/Mac:
> openssl rand -hex 32
> ```

---

## Iniciar localmente (sem Docker)

Requer PostgreSQL 16 rodando localmente com as credenciais do `application.yml` (`neurogen`/`secret`/`neurogen`) na porta `5433`.

```powershell
# Compilar sem testes
.\mvnw clean package -DskipTests

# Rodar
.\mvnw spring-boot:run
```

---

## Executar os testes

### Suite completa

```powershell
.\mvnw test
```

Resultado esperado: **90 testes, 0 falhas, 0 erros** em 18 classes de teste.

### Por classe

```powershell
.\mvnw test -Dtest=AuthControllerTest
.\mvnw test -Dtest=AuthenticationUseCaseTest
.\mvnw test -Dtest=RegistrationUseCaseTest
.\mvnw test -Dtest=TaskUseCaseTest
.\mvnw test -Dtest=NeuroGenIntegrationTest
```

### Por método

```powershell
.\mvnw test -Dtest=AuthControllerTest#loginInvalidCredentialsReturns401
```

### Cobertura por módulo

| Classe | Tipo | Testes |
|---|---|---|
| `EmailTest` | Domínio — shared | 3 |
| `AesGcmCipherTest` | Segurança — shared | 10 |
| `GlobalExceptionHandlerTest` | API — shared | 7 |
| `PasswordTest` | Domínio — identity | 3 |
| `UserTest` | Domínio — identity | 4 |
| `RegistrationUseCaseTest` | Aplicação — identity | 2 |
| `AuthenticationUseCaseTest` | Aplicação — identity | 4 |
| `AuthControllerTest` | API — identity | 3 |
| `SupportLinkTest` | Domínio — link | 6 |
| `TimeRangeTest` | Domínio — agenda | 2 |
| `EventTest` | Domínio — agenda | 8 |
| `TaskTest` (Criacao + Transicoes) | Domínio — agenda | 16 |
| `TaskUseCaseTest` | Aplicação — agenda | 9 |
| `FocusSessionTest` | Domínio — focus | 4 |
| `MoodScoreTest` | Domínio — emotional | 2 |
| `EncryptedPayloadTest` | Domínio — vault | 3 |
| `MasterKeyMetaTest` | Domínio — vault | 2 |
| `NeuroGenIntegrationTest` | Integração (Testcontainers) | 2 |
| **Total** | | **90** |

> **Teste de integração:** o `NeuroGenIntegrationTest` usa **Testcontainers** — sobe um PostgreSQL 16 real, aplica todas as migrations Flyway e valida que contas `APOIO` recebem 403 no Cofre e a tentativa é registrada na auditoria. Requer Docker rodando.

### Testes manuais com curl

Ver os arquivos de integração de cada módulo nesta pasta:

- [api-identity.md](api-identity.md) — Autenticação e perfil
- [api-link.md](api-link.md) — Vínculo Titular/Apoio
- [api-vault.md](api-vault.md) — Cofre zero-knowledge
- [api-agenda.md](api-agenda.md) — Eventos e Tarefas
- [api-focus.md](api-focus.md) — Sessões de foco
- [api-emotional.md](api-emotional.md) — Regulação emocional
- [api-audit.md](api-audit.md) — Auditoria imutável (consulta e referência)

### Swagger UI

Com a aplicação no ar: http://localhost:8080/swagger-ui.html

Clique em **Authorize** (canto superior direito) e informe o access token no formato `Bearer <token>` para testar endpoints protegidos diretamente pelo browser.

---

## Variáveis de ambiente

| Variável | Padrão (dev) | Descrição |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `default` | Use `prod` em produção |
| `DB_HOST` | `localhost` | Host do PostgreSQL |
| `DB_PORT` | `5432` | Porta interna Docker (5433 na máquina host) |
| `DB_NAME` | `neurogen` | Nome do banco |
| `DB_USERNAME` | `neurogen` | Usuário do banco |
| `DB_PASSWORD` | `secret` | Senha do banco |
| `DB_POOL_SIZE` | `10` | Tamanho do pool Hikari core |
| `VAULT_DB_URL` | _(mesmo banco)_ | URL do banco do Vault (isolamento físico em prod) |
| `VAULT_DB_USERNAME` | _(mesmo usuário)_ | Usuário Vault |
| `VAULT_DB_PASSWORD` | _(mesma senha)_ | Senha Vault |
| `VAULT_DB_POOL_SIZE` | `5` | Tamanho pool Vault |
| `SENSITIVE_DATA_KEY` | _(somente dev)_ | Chave AES-256 hex 64 chars — **OBRIGATÓRIA em prod** |
| `PORT` | `8080` | Porta HTTP (Render/Railway injetam automaticamente) |

---

## CORS — integração com frontend

O backend tem CORS configurado para frontends locais. As origens permitidas em desenvolvimento são:

```
http://localhost:3000   # Next.js / Create React App
http://localhost:5173   # Vite
```

Para adicionar outras origens, edite `application.yml`:

```yaml
neurogen:
  cors:
    allowed-origins:
      - http://localhost:3000
      - http://localhost:5173
      - https://seu-dominio.com
```

Em produção, defina a variável de ambiente `NEUROGEN_CORS_ALLOWED_ORIGINS` com as origens separadas por vírgula, ou sobrescreva via `application-prod.yml`.

> **Importante:** se o login retornar `401 INVALID_CREDENTIALS` ao usar o frontend pela primeira vez, o banco pode estar vazio. Registre um usuário primeiro via `POST /api/v1/auth/register` antes de tentar o login.

---

## Endpoints por módulo

| Módulo | Base URL | Autenticação | Referência |
|---|---|---|---|
| Autenticação | `/api/v1/auth` | Pública (exceto logout) | [api-identity.md](api-identity.md) |
| Perfil | `/api/v1/users` | JWT Bearer | [api-identity.md](api-identity.md) |
| Vínculo | `/api/v1/links` | JWT Bearer | [api-link.md](api-link.md) |
| Cofre | `/api/v1/vault` | JWT Bearer (somente TITULAR) | [api-vault.md](api-vault.md) |
| Agenda — Eventos | `/api/v1/events` | JWT Bearer | [api-agenda.md](api-agenda.md) |
| Agenda — Tarefas | `/api/v1/tasks` | JWT Bearer | [api-agenda.md](api-agenda.md) |
| Foco | `/api/v1/focus` | JWT Bearer | [api-focus.md](api-focus.md) |
| Emocional | `/api/v1/emotional` | JWT Bearer | [api-emotional.md](api-emotional.md) |
| Auditoria | `ng_audit.*` (banco) | — consulta direta | [api-audit.md](api-audit.md) |

---

## Fluxo de autenticação

```
POST /api/v1/auth/register      →  cria conta (PENDING_VERIFICATION)
POST /api/v1/auth/verify-email  →  ativa conta (ACTIVE)
POST /api/v1/auth/login         →  retorna { accessToken, refreshToken, expiresIn }
```

> Em desenvolvimento, o token de verificação de e-mail é **exibido no console** da aplicação (LoggingEmailSender). Procure por `[EMAIL]` nos logs após o registro.

> Contas com status `PENDING_VERIFICATION` conseguem fazer login — a verificação de e-mail não é pré-requisito obrigatório para autenticação.

Se TOTP estiver habilitado, o login retorna `403 TotpRequired`; reenvie com o campo `totpCode` preenchido:

```json
{ "email": "...", "password": "...", "totpCode": "123456" }
```

```
POST /api/v1/auth/refresh    →  renova tokens (sem reautenticar)
POST /api/v1/auth/logout     →  revoga o refreshToken
```

O `accessToken` tem validade de **15 min**. Inclua-o em todas as chamadas protegidas:

```
Authorization: Bearer <accessToken>
```

---

## Auditoria

Toda operação crítica fica registrada automaticamente em dois lugares:

| Tabela | Alimentada por | O que registra |
|---|---|---|
| `ng_audit.ng_db_audit` | Triggers PostgreSQL (síncrono) | INSERT/UPDATE/DELETE nas tabelas críticas — colunas sensíveis são omitidas automaticamente |
| `ng_audit.ng_system_logs` | `AuditService` Java (`@Async`) | Eventos de domínio, login/logout, erros HTTP |

Ambas as tabelas são **imutáveis**: UPDATE e DELETE lançam exceção no banco (`SQLSTATE 55000`).

Para consultar via pgAdmin ou psql:

```sql
-- Últimas 50 operações de banco
SELECT occurred_at, operation, table_name, record_id, app_user_id
FROM ng_audit.ng_db_audit
ORDER BY occurred_at DESC
LIMIT 50;

-- Últimas falhas de segurança
SELECT logged_at, action, user_id, ip_address, metadata
FROM ng_audit.ng_system_logs
WHERE level = 'WARN' AND action IS NOT NULL
ORDER BY logged_at DESC
LIMIT 50;

-- Tentativas de acesso ao Vault por contas APOIO
SELECT occurred_at, record_id, app_user_id, operation
FROM ng_audit.ng_db_audit
WHERE table_name = 'ng_vault_auth_logs'
ORDER BY occurred_at DESC;
```
