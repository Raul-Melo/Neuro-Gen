<div align="center">
  <img src="./public/Neuro-Gen_v2.png" alt="Neuro-Gen Logo" width="140"/>

  <h1>Neuro-Gen</h1>
  <p><strong>Agenda adaptativa para mentes neurodivergentes</strong></p>

  <p>
    <img src="https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk&logoColor=white"/>
    <img src="https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen?style=flat-square&logo=springboot&logoColor=white"/>
    <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql&logoColor=white"/>
    <img src="https://img.shields.io/badge/Flyway-10-CC0200?style=flat-square&logo=flyway&logoColor=white"/>
    <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white"/>
    <img src="https://img.shields.io/badge/JWT-OAuth2-black?style=flat-square&logo=jsonwebtokens&logoColor=white"/>
    <img src="https://img.shields.io/badge/Status-Em%20Desenvolvimento-yellow?style=flat-square"/>
  </p>
  <p>
    <a href="https://github.com/Raul-Melo/Neuro-Gen/actions/workflows/ci.yml">
      <img src="https://github.com/Raul-Melo/Neuro-Gen/actions/workflows/ci.yml/badge.svg?branch=main" alt="CI Status"/>
    </a>
  </p>
</div>

---

## Sumário

- [Sobre o Projeto](#sobre-o-projeto)
- [Stacks e Tecnologias](#stacks-e-tecnologias)
- [Arquitetura e Princípios](#arquitetura-e-princípios)
- [Banco de Dados](#banco-de-dados)
- [Segurança](#segurança)
- [Como Executar — Local](#como-executar--local)
- [Como Executar — Nuvem](#como-executar--nuvem)
- [Testes](#testes)
- [Documentação](#documentação)
- [Equipe](#equipe)

---

## Sobre o Projeto

**Neuro-Gen** é uma agenda-planner adaptativa para pessoas com neurodivergências — TDAH, TEA e Superdotação (2E). Desenvolvido como projeto acadêmico da disciplina **Projeto de Software 2** na **SentinelCorp**, o sistema resolve três falhas centrais dos aplicativos de agenda genéricos:

| Problema | Como o Neuro-Gen resolve |
|---|---|
| **Função executiva** — dificuldade em iniciar tarefas, estimar tempo, manter atenção | Lembretes ativos e adaptativos, timers de foco (Pomodoro), subtarefas granulares, prioridade explícita |
| **Regulação emocional** — sobrecarga não percebida, burnout silencioso | Check-ins diários de humor e energia, diário emocional cifrado, dashboard de tendência |
| **Suporte externo** — cuidadores sem visibilidade, sem invadir privacidade | Sistema de Vínculo Titular/Apoio com permissões granulares e opt-in por módulo |

### Perfis de usuário

- **Titular** — o próprio usuário neurodivergente. Dono de todos os dados. Define o que cada Apoio pode ver.
- **Apoio** — pai/mãe, terapeuta, parceiro(a). Acesso somente ao que o Titular autorizar. **Nunca** acessa o Cofre.

---

## Stacks e Tecnologias

### Backend

| Tecnologia | Versão | Papel |
|---|---|---|
| **Java** | 21 LTS | Linguagem principal — virtual threads (Project Loom) |
| **Spring Boot** | 3.3.5 | Framework web (REST, DI, configuração) |
| **Spring Security** | 6.3 | Autenticação e autorização; 3 filter chains |
| **Spring Authorization Server** | 1.3 | Emissão e assinatura de tokens JWT (RSA-2048, JWKS) |
| **Spring Data JPA / Hibernate** | 6.5 | ORM — adaptadores de porta do domínio |
| **Flyway** | 10 | Versionamento do schema; Core e Vault com Flyway próprios |
| **springdoc-openapi** | 2.6 | Documentação OpenAPI 3.1 / Swagger UI |
| **Bouncy Castle** | 1.78.1 | Argon2id para hash de senha (OWASP: m=19 MB, t=2, p=1) |
| **dev.samstevens.totp** | 1.7.1 | Autenticação de dois fatores (TOTP / RFC 6238) |
| **Spring Boot Actuator** | — | Health check, liveness/readiness probes |
| **Lombok** | — | Redução de boilerplate |

### Banco de Dados e Infra

| Tecnologia | Papel |
|---|---|
| **PostgreSQL 16** | Banco principal (Core) e banco isolado do Vault (ADR-005) |
| **HikariCP** | Pool de conexões — `core-pool` (máx. 10) e `vault-pool` (máx. 5) |
| **Docker / Docker Compose** | Banco local de desenvolvimento + build de produção |
| **GitHub Actions** | CI: build + 90 testes a cada push/PR (portão do deploy) |
| **Render** | Hospedagem em nuvem — Web Service Docker + PostgreSQL gerenciado |

### Qualidade

| Ferramenta | Uso |
|---|---|
| **JUnit 5 + AssertJ** | Testes de domínio (VOs, agregados, invariantes) |
| **Mockito** | Testes de caso de uso (mocks de portas) |
| **MockMvc** | Testes de API — contratos HTTP e RFC 7807 |
| **Testcontainers** | Teste de integração com PostgreSQL 16 real + Flyway |
| **Maven Wrapper** | Build sem instalação de Maven — `./mvnw` |

---

## Arquitetura e Princípios

O projeto segue **arquitetura hexagonal (Ports & Adapters)** combinada com **DDD** em um **monólito modular**. Cada módulo é um bounded context verticalmente isolado.

### Estrutura por módulo

```
br.com.sentinelcorp.Neuro_Gen.
├── shared/          ← kernel: tipos base, exceções, segurança, auditoria, config
├── identity/        ← autenticação, JWT, TOTP, verificação de e-mail, perfil
├── link/            ← vínculo Titular/Apoio, convites, permissões granulares
├── agenda/          ← eventos (RRULE), tarefas, subtarefas, lembretes (nudge)
├── focus/           ← sessões Pomodoro com pausa/retomada
├── emotional/       ← check-ins de humor, diário cifrado
└── vault/           ← cofre zero-knowledge, DataSource isolado
```

Cada módulo é dividido em quatro camadas:

```
<módulo>/
  domain/          ← Java puro: modelo, eventos, exceções, portas (interfaces)
  application/     ← casos de uso; dependem somente de portas
  infrastructure/  ← adaptadores Spring: JPA entities, repos, service impls
  api/             ← @RestController + DTOs
```

### Módulos e endpoints

| Módulo | Base URL | Responsabilidade |
|---|---|---|
| `identity` | `/api/v1/auth`, `/api/v1/users` | Registro, login (JWT + TOTP), verificação de e-mail, reset de senha, perfil |
| `link` | `/api/v1/links` | Convite, aceite, revogação, permissões granulares por módulo |
| `agenda` | `/api/v1/events`, `/api/v1/tasks` | Eventos com recorrência RRULE, tarefas, subtarefas, lembretes adaptativos |
| `focus` | `/api/v1/focus` | Sessões Pomodoro — start, pause, resume, end |
| `emotional` | `/api/v1/emotional` | Check-in diário de humor/energia, diário emocional cifrado |
| `vault` | `/api/v1/vault` | Cofre zero-knowledge — setup de chave mestra, abertura, entradas cifradas |

### Decisões arquiteturais (ADRs)

| ADR | Decisão | Status |
|---|---|---|
| ADR-001 | Monolito modular + Vault isolado | Aceito |
| ADR-002 | Java 21 + Spring Boot 3 + PostgreSQL | Aceito |
| ADR-005 | DataSource isolado para o Vault | Aceito |
| ADR-006 | AES-256-GCM em `ng_sensitive` | Aceito |
| ADR-007 | Deploy em Render via Docker multi-stage | Aceito |
| ADR-008 | TDD + CI obrigatório | Aceito |

Texto completo: [`docs/defContexto/05-adrs.md`](docs/defContexto/05-adrs.md)

---

## Banco de Dados

PostgreSQL 16 com **dois DataSources independentes** e **cinco schemas**, cada um representando um nível de sensibilidade de dado.

### Schemas

| Schema | Propósito | Sensibilidade |
|---|---|---|
| `ng_identity` | Autenticação, sessões, tokens, consentimento | Alta — senhas como Argon2id, tokens como SHA-256 |
| `ng_core` | Dados operacionais: agenda, vínculos, foco, perfis | Média — dados funcionais do produto |
| `ng_sensitive` | Saúde mental: humor, diário emocional | Máxima — LGPD art. 5º, II; cifrado AES-256-GCM |
| `ng_audit` | Auditoria imutável: `ng_db_audit` + `ng_system_logs` | Especial — append-only, UPDATE/DELETE bloqueados no banco |
| `ng_vault` | Cofre zero-knowledge de credenciais | Máxima — banco separado em produção (ADR-005) |

> O prefixo `ng_` em todos os schemas e tabelas é a abreviação de **Neuro-Gen** e evita colisões de nome em ambientes compartilhados. Detalhes em [`docs/banco-de-dados.md`](docs/banco-de-dados.md).

### Migrations (Flyway)

```
src/main/resources/db/
├── migration/
│   ├── V1__schema_identity.sql   ← ng_identity: users, sessions, tokens, consent
│   ├── V2__schema_core.sql       ← ng_core: links, agenda, focus, notifications
│   ├── V3__schema_sensitive.sql  ← ng_sensitive: mood, journal, neurodivergence
│   ├── V4__indexes.sql           ← índices de performance (RNF-ESC.4)
│   └── V5__schema_audit.sql      ← ng_audit: triggers imutáveis, system logs
└── vault/migration/
    └── V1__schema_vault.sql      ← ng_vault: entries, auth logs, master key meta
```

**Regra:** migrations aplicadas são imutáveis. Para corrigir um schema em produção, crie uma nova migration — nunca edite arquivos existentes.

### Dois DataSources

```
Aplicação
 ├── CoreDataSource  (HikariCP pool=10)
 │    └── schemas: ng_identity, ng_core, ng_sensitive, ng_audit
 └── VaultDataSource (HikariCP pool=5)
      └── schema: ng_vault  ←  instância PostgreSQL separada em produção
```

Em desenvolvimento ambos usam o mesmo banco. Em produção, configure `VAULT_DB_URL`, `VAULT_DB_USERNAME` e `VAULT_DB_PASSWORD` para isolamento físico.

### Auditoria imutável

Toda operação crítica fica registrada em dois lugares:

| Tabela | Alimentada por | Timing |
|---|---|---|
| `ng_audit.ng_db_audit` | Triggers PostgreSQL (`fn_capture_dml`) | Síncrono — mesma transação |
| `ng_audit.ng_system_logs` | `AuditService` Java (`@Async`) | Assíncrono — thread separada |

Ambas as tabelas têm triggers `BEFORE UPDATE OR DELETE` que lançam `SQLSTATE 55000` — nem mesmo o superusuário do banco consegue alterar ou apagar um registro de auditoria em uma sessão normal.

Referência completa: [`docs/banco-de-dados.md`](docs/banco-de-dados.md) · [`docs/integracao/api-audit.md`](docs/integracao/api-audit.md)

---

## Segurança

O Neuro-Gen implementa defesa em profundidade — cada camada protege independentemente das demais.

### Autenticação e tokens

| Mecanismo | Detalhe |
|---|---|
| **JWT stateless** | Access token RSA-2048, TTL 15 min; emitido pelo Spring Authorization Server |
| **Refresh token** | Opaco, armazenado como SHA-256 no banco; TTL 30 dias; revogável |
| **TOTP (2FA)** | RFC 6238; segredo cifrado AES-256-GCM em repouso |
| **Senhas** | Argon2id — m=19.456 KB, t=2, p=1 (parâmetros OWASP) via BouncyCastle |

### Controle de acesso

Três filter chains do Spring Security com `@Order` explícita:

```
Order 1 — Authorization Server  →  /oauth2/**, /api/v1/auth/**  (público)
Order 2 — Vault (estrutural)     →  /api/v1/vault/**            (somente TITULAR)
Order 3 — API padrão             →  /**                         (JWT Bearer)
```

A negação do Vault a contas `APOIO` é **estrutural** — implementada no filtro, não em lógica de negócio. Tentativas são registradas em `ng_vault_auth_logs` via `VaultAccessDeniedHandler`.

### Proteção de dados em repouso

| Dado | Proteção |
|---|---|
| Notas de humor e diário emocional | AES-256-GCM cifrado na aplicação; armazenado como `BYTEA` em `ng_sensitive` |
| Segredo TOTP | AES-256-GCM em `ng_identity.ng_totp_secrets` |
| Entradas do cofre | AES-256-GCM com `ciphertext` + `iv` + `salt` individuais por entrada em `ng_vault` |
| Senhas e tokens | Nunca armazenados em texto claro — apenas Argon2id/SHA-256 |
| Colunas de credencial no audit log | Removidas automaticamente por `fn_capture_dml_redacted` antes de gravar |

### Chave de dados sensíveis

A chave AES-256 (`SENSITIVE_DATA_KEY`) deve ser um hex de 64 caracteres (32 bytes). Em produção, **nunca** coloque no código — injete via variável de ambiente:

```bash
# Gerar uma chave segura
openssl rand -hex 32
```

> **Atenção:** troque a chave antes de ter dados em produção. Dados cifrados com a chave antiga se tornam ilegíveis se a chave for alterada depois.

### Privacidade e LGPD

| Requisito | Implementação |
|---|---|
| Consentimento explícito (art. 7º, I) | `ng_identity.ng_consent_records` — append-only, versão + timestamp + IP |
| Dado sensível de saúde (art. 5º, II) | Schema `ng_sensitive` separado + cifração AES-256-GCM |
| Registro de operações (art. 37) | `ng_audit.ng_db_audit` + `ng_audit.ng_system_logs` |
| Retenção mínima | 12 meses para logs de auditoria (RNF-SEC.8) |
| Soft-delete com anonimização | `ng_users.deleted_at` — dados retidos pelo período legal, depois anonimizados |

---

## Como Executar — Local

### Pré-requisitos

| Ferramenta | Versão mínima |
|---|---|
| Docker Desktop | 4.x — deve estar **rodando** antes de qualquer comando |
| Java JDK | 21 |
| Maven | 3.9+ (ou use o wrapper `.\mvnw` incluído) |

> **Windows:** se o PostgreSQL local estiver instalado, ele já ocupa a porta 5432. O Docker do projeto usa a porta **5433** para evitar esse conflito.

---

### Modo 1 — Banco no Docker + aplicação local (recomendado para desenvolvimento)

```powershell
# 1. Clone o repositório
git clone https://github.com/Raul-Melo/Neuro-Gen.git
cd Neuro-Gen

# 2. Suba o PostgreSQL e pgAdmin
docker compose up -d

# 3. Aguarde o banco ficar saudável
docker compose ps   # aguarde "healthy" na coluna STATUS

# 4. Rode a aplicação (Flyway aplica todas as migrations automaticamente)
.\mvnw spring-boot:run
```

**Serviços disponíveis:**

| Serviço | URL | Credenciais |
|---|---|---|
| API REST | `http://localhost:8080` | — |
| Swagger UI | `http://localhost:8080/swagger-ui.html` | — |
| Health Check | `http://localhost:8080/actuator/health` | — |
| pgAdmin | `http://localhost:5050` | `admin@admin.com` / `admin` |
| PostgreSQL | `localhost:5433` | `neurogen` / `secret` / db: `neurogen` |

**Conexão no pgAdmin** (adicionar servidor manualmente):
- **Host:** `localhost` · **Porta:** `5433` · **Banco:** `neurogen` · **Usuário:** `neurogen` · **Senha:** `secret`

---

### Modo 2 — Stack completa no Docker (simula produção)

Constrói a imagem da aplicação e sobe tudo junto: banco + pgAdmin + app.

```powershell
# 1. Copie e preencha o arquivo de variáveis
cp .env.example .env
# Edite .env — defina DB_PASSWORD e SENSITIVE_DATA_KEY (obrigatórias)

# 2. Gere uma SENSITIVE_DATA_KEY segura
# Windows PowerShell:
-join ((1..32) | ForEach-Object { '{0:x2}' -f (Get-Random -Max 256) })
# Linux / Mac:
# openssl rand -hex 32

# 3. Suba tudo (build + banco + app)
docker compose -f docker-compose.full.yml --env-file .env up --build -d

# 4. Acompanhe os logs
docker compose -f docker-compose.full.yml logs -f app

# 5. Para encerrar
docker compose -f docker-compose.full.yml down
```

---

### Variáveis de ambiente

| Variável | Padrão (dev) | Descrição |
|---|---|---|
| `DB_HOST` | `localhost` | Host do PostgreSQL |
| `DB_PORT` | `5432` | Porta interna Docker (5433 na máquina host) |
| `DB_NAME` | `neurogen` | Nome do banco |
| `DB_USERNAME` | `neurogen` | Usuário do banco |
| `DB_PASSWORD` | `secret` | Senha do banco |
| `DB_POOL_SIZE` | `10` | Tamanho do pool Hikari core |
| `VAULT_DB_URL` | _(mesmo banco)_ | URL do banco do Vault — isolamento em produção |
| `VAULT_DB_USERNAME` | _(mesmo usuário)_ | Usuário do banco Vault |
| `VAULT_DB_PASSWORD` | _(mesma senha)_ | Senha do banco Vault |
| `SENSITIVE_DATA_KEY` | _(somente dev)_ | Chave AES-256 hex 64 chars — **obrigatória em produção** |
| `PORT` | `8080` | Porta HTTP (Render injeta automaticamente) |
| `SPRING_PROFILES_ACTIVE` | `default` | Use `prod` em produção |

---

### Comandos de desenvolvimento

```powershell
# Compilar e empacotar (sem testes)
.\mvnw clean package -DskipTests

# Compilar e empacotar (com testes)
.\mvnw clean package

# Rodar todos os testes
.\mvnw test

# Rodar uma classe de teste específica
.\mvnw test -Dtest=AuthControllerTest

# Rodar um método de teste específico
.\mvnw test -Dtest=AuthControllerTest#loginInvalidCredentialsReturns401
```

---

## Como Executar — Nuvem

### Render (configuração atual — ADR-007)

O deploy é automatizado: todo merge na `main` com CI verde publica automaticamente no Render.

```
git push / merge na main
        ↓
GitHub Actions (./mvnw verify — 90 testes)
        ↓ passa
Render autoDeployTrigger: checksPass
        ↓
docker build (Dockerfile multi-stage)
        ↓
Web Service Spring Boot (perfil prod) + PostgreSQL 16 gerenciado
```

#### Primeira implantação no Render (~15 min)

**1.** Certifique-se de que o CI está verde na branch `main`.

**2.** Crie uma conta em [render.com](https://render.com) usando **Sign in with GitHub** e autorize o repositório `Neuro-Gen`.

**3.** No painel: **New → Blueprint** → selecione o repositório. O Render lê o `render.yaml` e exibe o que será criado (`neurogen-db` e `neuro-gen-api`). Clique em **Apply**.

**4.** Aguarde o primeiro build (5–10 min). Acompanhe em *neuro-gen-api → Logs*. O deploy termina quando aparecer `Started NeuroGenApplication`.

**5.** Verifique os endpoints:
```
https://<sua-url>.onrender.com/actuator/health    →  {"status":"UP"}
https://<sua-url>.onrender.com/swagger-ui.html    →  documentação interativa
```

#### Variáveis injetadas pelo `render.yaml`

| Variável | Origem | Detalhe |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `render.yaml` | `prod` |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | `fromDatabase` | Injetadas do PostgreSQL gerenciado |
| `SENSITIVE_DATA_KEY` | `generateValue` | Gerada automaticamente no primeiro deploy — **não troque depois de ter dados** |
| `PORT` | Plataforma | Render injeta a porta automaticamente |

#### Problemas comuns no Render

| Sintoma | Causa | Solução |
|---|---|---|
| Deploy não dispara após merge | CI falhou ou está rodando | Veja *Actions* no GitHub; corrija e faça novo push |
| Primeira requisição demora minutos | Serviço hibernou (plano gratuito) | Normal — abra a URL 5 min antes de apresentar |
| `401` com token que funcionava | Serviço reiniciou, gerou novo par RSA | Faça login novamente |
| `SENSITIVE_DATA_KEY` ausente | Variável não configurada | Confira *Environment* do serviço no Render |
| Banco indisponível após ~30 dias | PostgreSQL gratuito expirou | Recrie pelo Blueprint ou faça upgrade |

---

### Lovable (front-end)

O Neuro-Gen é um **backend REST**. Para conectar um front-end gerado no Lovable:

**1.** No projeto Lovable, configure a URL base da API no arquivo de ambiente:

```env
VITE_API_BASE_URL=https://<sua-url>.onrender.com
```

**2.** O CORS já está configurado para aceitar origens locais e pode ser expandido:

```yaml
# application.yml
neurogen:
  cors:
    allowed-origins:
      - http://localhost:3000
      - http://localhost:5173
      - https://neurogen.lovable.app   # adicione aqui
```

**3.** Fluxo de autenticação para o front-end:

```
POST /api/v1/auth/register   →  cria conta
POST /api/v1/auth/login      →  retorna { accessToken, refreshToken, expiresIn }
Authorization: Bearer <accessToken>  →  use em todas as chamadas protegidas
POST /api/v1/auth/refresh    →  renova tokens (access token expira em 15 min)
POST /api/v1/auth/logout     →  revoga o refreshToken
```

**4.** O token de verificação de e-mail é exibido nos **logs da aplicação** (procure por `[EMAIL]`) — o envio real de e-mail ainda usa `LoggingEmailSender` em desenvolvimento.

---

### Vercel (front-end)

Para fazer deploy de um front-end no Vercel apontando para esta API:

**1.** No painel da Vercel, em **Settings → Environment Variables**, adicione:

```
NEXT_PUBLIC_API_URL = https://neurogen.onrender.com
```

**2.** Adicione a URL do Vercel na lista de origens permitidas no CORS (via variável de ambiente ou `application.yml`):

```yaml
neurogen:
  cors:
    allowed-origins:
      - https://neurogen.vercel.app
```

**3.** Se o front-end usa **Next.js com App Router** e faz chamadas server-side (Server Components / Route Handlers), não há problema de CORS — as chamadas partem do servidor Vercel, não do browser. Nesse caso, configure a variável sem o prefixo `NEXT_PUBLIC_`:

```
API_URL = https://neurogen.onrender.com
```

**4.** A Vercel faz deploy automático a partir de qualquer branch que você conectar no painel. Recomenda-se conectar à branch `main` (mesmo padrão do Render) para manter front e back sincronizados.

---

## Testes

```powershell
.\mvnw test
```

Resultado esperado: **90 testes, 0 falhas, 0 erros** em 18 classes.

| Classe | Tipo | Nº |
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
| `TaskTest` | Domínio — agenda | 16 |
| `TaskUseCaseTest` | Aplicação — agenda | 9 |
| `FocusSessionTest` | Domínio — focus | 4 |
| `MoodScoreTest` | Domínio — emotional | 2 |
| `EncryptedPayloadTest` | Domínio — vault | 3 |
| `MasterKeyMetaTest` | Domínio — vault | 2 |
| `NeuroGenIntegrationTest` | Integração (Testcontainers) | 2 |
| **Total** | | **90** |

> O `NeuroGenIntegrationTest` sobe um PostgreSQL 16 real via **Testcontainers**, aplica todas as migrations Flyway e valida que contas `APOIO` recebem 403 no Cofre com a tentativa auditada. Requer Docker rodando.

---

## Documentação

| Documento | Conteúdo |
|---|---|
| [`docs/banco-de-dados.md`](docs/banco-de-dados.md) | Modelagem completa do banco: prefixo `ng_`, schemas, decisões de design, todas as tabelas, índices, tipos de dado |
| [`docs/defContexto/`](docs/defContexto/) | Visão do produto, requisitos funcionais, RNFs, arquitetura geral, ADRs |
| [`docs/integracao/README.md`](docs/integracao/README.md) | Getting started, fluxo de autenticação, tabela de endpoints, variáveis |
| [`docs/integracao/api-audit.md`](docs/integracao/api-audit.md) | Referência da auditoria: queries forenses, ações registradas, imutabilidade |
| [`docs/diagramas/02-der.md`](docs/diagramas/02-der.md) | DER visual (Mermaid) de todos os schemas |
| [`docs/diagramas/`](docs/diagramas/) | Diagramas de classes, casos de uso e sequência (PlantUML) |
| [`docs/deploy/README.md`](docs/deploy/README.md) | Guia de implantação no Render, roteiro de demonstração |

---

## Equipe

<table align="center">
  <tr>
    <td align="center">
      <b>Raul Fernandes Silva Melo</b><br/>
      <sub>Desenvolvedor</sub>
    </td>
    <td align="center">
      <b>Bruna Duarte Bueno</b><br/>
      <sub>Desenvolvedora</sub>
    </td>
    <td align="center">
      <b>Matheus Lemes Carneiro</b><br/>
      <sub>Desenvolvedor</sub>
    </td>
  </tr>
</table>

<br/>

<div align="center">
  <b>Professor Supervisor:</b> Marcus Artiaga Colantoni<br/>
  <sub>Disciplina: Projeto de Software 2 — Sentinel · 2026</sub>
</div>

---

<div align="center">
  <sub>Desenvolvido com dedicação por <strong>Sentinel</strong></sub>
</div>
