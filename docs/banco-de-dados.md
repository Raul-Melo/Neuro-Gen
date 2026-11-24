# Banco de Dados — Modelagem, Estrutura e Decisões

> **Sistema:** Neuro-Gen · SentinelCorp  
> **SGBD:** PostgreSQL 16  
> **Gerenciador de migrations:** Flyway  
> **Última atualização:** 2026-11-24

---

## Sumário

1. [Filosofia de nomenclatura](#1-filosofia-de-nomenclatura)
2. [Estratégia de schemas](#2-estratégia-de-schemas)
3. [Dois bancos, dois datasources](#3-dois-bancos-dois-datasources)
4. [Gestão de migrations com Flyway](#4-gestão-de-migrations-com-flyway)
5. [Convenções de modelagem](#5-convenções-de-modelagem)
6. [Schema ng_identity](#6-schema-ng_identity)
7. [Schema ng_core](#7-schema-ng_core)
8. [Schema ng_sensitive](#8-schema-ng_sensitive)
9. [Schema ng_audit](#9-schema-ng_audit)
10. [Schema ng_vault (banco separado)](#10-schema-ng_vault-banco-separado)
11. [Estratégia de índices](#11-estratégia-de-índices)
12. [Tipos de dado e justificativas](#12-tipos-de-dado-e-justificativas)
13. [Decisões de design registradas](#13-decisões-de-design-registradas)
14. [Referências cruzadas](#14-referências-cruzadas)

---

## 1. Filosofia de nomenclatura

### O prefixo `ng_`

Todos os schemas e todas as tabelas do projeto carregam o prefixo `ng_`, abreviação de **Neuro-Gen**.

**Por quê?**

PostgreSQL não isola namespaces de tabelas entre conexões do mesmo banco — apenas schemas fazem isso. Em um banco compartilhado (ambiente dev, staging) onde outros projetos ou ferramentas (pgAdmin, extensões, ferramentas de BI) também criam objetos, nomes genéricos como `users`, `sessions` ou `events` colidem inevitavelmente.

O prefixo `ng_` resolve isso em dois níveis:

| Nível | Problema evitado | Exemplo |
|---|---|---|
| **Schema** | Colisão de namespace entre projetos no mesmo servidor PostgreSQL | `ng_identity` vs `public` |
| **Tabela** | Colisão de nome dentro do mesmo schema quando qualificação é omitida | `ng_users` vs `users` |

Isso também torna buscas no banco inequívocas: `SELECT * FROM ng_` retorna exclusivamente objetos do Neuro-Gen, sem ambiguidade com objetos de sistema (`pg_*`) ou de outros projetos.

### Padrão de nomenclatura completo

```
ng_<schema>.<ng_tabela>
│    │           │
│    │           └── prefixo ng_ + nome descritivo em snake_case singular
│    └────────────── nome do schema: contexto de negócio
└─────────────────── prefixo do projeto (Neuro-Gen)
```

**Exemplos:**
```
ng_identity.ng_users            ← schema de identidade, tabela de usuários
ng_core.ng_support_links        ← schema operacional, tabela de vínculos
ng_sensitive.ng_mood_checkins   ← schema de dados sensíveis, check-ins de humor
ng_audit.ng_db_audit            ← schema de auditoria, log de banco
ng_vault.ng_vault_entries       ← schema do cofre, entradas cifradas
```

> **Nota de código:** o pacote Java usa `Neuro_Gen` (sublinhado) por restrição da linguagem — `Neuro-Gen` com hífen é inválido como identificador Java. O banco usa snake_case padrão.

---

## 2. Estratégia de schemas

O banco é particionado em **cinco schemas**, cada um representando um nível de sensibilidade e um contexto de negócio distinto:

```
neurogen (banco)
├── ng_identity      ← autenticação e controle de acesso
├── ng_core          ← dados operacionais (agenda, vínculos, foco)
├── ng_sensitive     ← dados sensíveis de saúde (LGPD art. 5º, II)
├── ng_audit         ← auditoria imutável (LGPD art. 37 / RNF-SEC.8)
└── ng_vault         ← cofre zero-knowledge (DataSource separado em prod)
```

### Por que schemas ao invés de bancos separados?

A separação por schemas dentro do mesmo banco oferece:

- **Referências de FK entre schemas** (`ng_core.ng_events` → `ng_identity.ng_users`) sem overhead de joins federados
- **Uma única conexão Flyway** para os quatro schemas principais (identity, core, sensitive, audit)
- **Controle de acesso via GRANT/REVOKE** por schema — papel de banco com `SELECT` em `ng_core` mas sem acesso a `ng_sensitive`
- **Backups consistentes** com um único `pg_dump` do banco

A exceção é o `ng_vault`, que usa um DataSource separado (ver seção 3).

### Hierarquia de sensibilidade

```
ng_identity  ─── Credenciais de autenticação (nunca em texto claro)
ng_core      ─── Dados funcionais do produto (baixa sensibilidade)
ng_sensitive ─── Dados sensíveis de saúde — cifrados em repouso (AES-256-GCM)
ng_audit     ─── Registro imutável — append-only, bloqueio de mutação no banco
ng_vault     ─── Cofre zero-knowledge — servidor nunca vê texto decifrado
```

---

## 3. Dois bancos, dois datasources

### Arquitetura de conexão

```
Aplicação Spring Boot
     │
     ├── CoreDataSource (HikariCP, pool=10)
     │       ├── schemas: ng_identity, ng_core, ng_sensitive, ng_audit
     │       ├── Flyway: classpath:db/migration (V1→V5)
     │       └── JPA: identity, link, agenda, focus, emotional, shared/audit
     │
     └── VaultDataSource (HikariCP, pool=5)
             ├── schema: ng_vault
             ├── Flyway: classpath:db/vault/migration (V1)
             └── JPA: vault (unidade de persistência isolada)
```

Em **desenvolvimento**, ambos apontam para o mesmo banco PostgreSQL (`jdbc:postgresql://localhost:5433/neurogen`). A separação de DataSource já garante isolamento de pool, transações e repositórios JPA.

Em **produção**, `VAULT_DB_URL`, `VAULT_DB_USERNAME` e `VAULT_DB_PASSWORD` devem ser configurados para uma instância PostgreSQL completamente separada — credenciais diferentes, host diferente, sem acesso cross-schema via `ng_vault.*`.

### Por que isso importa?

Isolamento físico do Vault significa que:
1. Um vazamento de credenciais do Core DB **não expõe** o banco do Vault
2. DBA com acesso ao Core não consegue ler entradas do cofre
3. Uma migration errada no Core não pode derrubar o schema do Vault por acidente
4. Métricas de pool, slow queries e auditoria de acesso são separadas

> Decisão registrada em **ADR-005**.

### Porta 5433 no desenvolvimento

O PostgreSQL 16 Docker usa a porta **5433** (mapeada internamente para 5432). Isso evita conflito com instalações locais do PostgreSQL que ocupam a porta padrão 5432 — comum em máquinas Windows onde o serviço `postgresql-x64-*` já está instalado.

---

## 4. Gestão de migrations com Flyway

### Localização dos arquivos

```
src/main/resources/
├── db/
│   ├── migration/              ← migrations do Core (Flyway padrão)
│   │   ├── V1__schema_identity.sql
│   │   ├── V2__schema_core.sql
│   │   ├── V3__schema_sensitive.sql
│   │   ├── V4__indexes.sql
│   │   └── V5__schema_audit.sql
│   └── vault/
│       └── migration/          ← migrations do Vault (Flyway separado)
│           └── V1__schema_vault.sql
```

### Convenção de nomes

```
V<versão>__<descricao_em_snake_case>.sql
│           │
│           └── Descrição breve em snake_case — sem acentos, sem espaços
└────────────── Número inteiro crescente; não pode haver gap nem duplicata
```

**Regra crítica:** migrations aplicadas **nunca podem ser alteradas**. O Flyway calcula um checksum de cada arquivo. Se o arquivo for modificado após aplicação, o próximo startup da aplicação falhará com `FlywayException: Validate failed`. Para corrigir um schema já em produção, crie uma nova migration `V6__...`.

### Schemas gerenciados

```yaml
# application.yml
spring:
  flyway:
    schemas: ng_identity,ng_core,ng_sensitive,ng_audit
    default-schema: ng_core
    create-schemas: true
```

O Flyway cria os schemas automaticamente se não existirem (`create-schemas: true`). A tabela de controle `flyway_schema_history` é criada no `default-schema` (`ng_core`).

O schema `ng_vault` tem seu próprio Flyway bean em `VaultDataSourceConfig` — localização `classpath:db/vault/migration`, schema `ng_vault`.

### Estratégia de DDL

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: none
```

**O Hibernate nunca cria nem altera tabelas.** O Flyway é o único dono do schema. Isso evita surpresas em produção onde o Hibernate poderia alterar uma coluna com base em uma mudança no modelo Java sem que o time percebesse.

---

## 5. Convenções de modelagem

### Chaves primárias

**Todas as PKs são `UUID`, geradas pela aplicação** (não pelo banco com `gen_random_uuid()`).

```sql
id UUID PRIMARY KEY   -- gerado em Java: UUID.randomUUID()
```

**Justificativa:** UUIDs gerados pela aplicação permitem que o domínio conheça o ID antes de persistir, o que é necessário para a publicação de eventos de domínio (`AggregateRoot.domainEvents()`). Se o banco gerasse a PK, o ID só estaria disponível após o `INSERT`, tornando a publicação de eventos pré-persistência impossível sem uma segunda query.

> Exceção: `ng_audit.ng_db_audit` e `ng_audit.ng_system_logs` usam `DEFAULT gen_random_uuid()` pois são preenchidas por triggers e pelo `AuditService` — fora do controle direto do domínio.

### Timestamps

| Coluna | Presente em | Semântica |
|---|---|---|
| `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()` | Todas as tabelas | Momento da criação do registro |
| `updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()` | Tabelas mutáveis | Última modificação; atualizado pelo JPA ou trigger |
| `deleted_at TIMESTAMPTZ` | `ng_users` | Soft-delete; `NULL` = conta ativa |
| `revoked_at TIMESTAMPTZ` | `ng_sessions` | Revogação de sessão; `NULL` = ativa |
| `cancelled_at TIMESTAMPTZ` | `ng_events` | Cancelamento de evento; `NULL` = ativo |
| `archived_at TIMESTAMPTZ` | `ng_tasks` | Arquivamento de tarefa; `NULL` = ativa |

**Por `TIMESTAMPTZ` e não `TIMESTAMP`?** `TIMESTAMPTZ` armazena o instante absoluto em UTC no PostgreSQL. Independente do fuso horário do servidor ou da JVM, a leitura é sempre consistente. O Hibernate é configurado com `hibernate.jdbc.time_zone: UTC` para garantir que nunca haja conversão implícita.

### Soft-delete vs hard-delete

O projeto usa **soft-delete seletivo**:

- `ng_users`: soft-delete via `deleted_at` (LGPD exige período de retenção antes de anonimizar)
- `ng_events`, `ng_tasks`: soft-delete via `cancelled_at`/`archived_at` (histórico de atividade)
- `ng_sessions`: revogação via `revoked_at` (nunca deletar — histórico de segurança)
- `ng_vault_entries`: hard-delete imediato ao excluir conta (RNF-RET.2 — dado do Vault eliminado irreversivelmente)

### Enums como VARCHAR

Nenhum tipo `ENUM` nativo do PostgreSQL é utilizado. Todos os valores enumerados são `VARCHAR` com `CHECK CONSTRAINT` onde necessário.

**Justificativa:** adicionar um valor a um `ENUM` nativo do PostgreSQL requer `ALTER TYPE ... ADD VALUE`, que não é transacional em versões anteriores ao PostgreSQL 12 e ainda causa lock em produção. Com `VARCHAR`, basta uma nova migration que adicione o valor à constraint — operação muito menos disruptiva.

### Dados JSONB

`JSONB` é usado para dados semi-estruturados onde o schema pode evoluir sem nova migration:

| Tabela | Coluna JSONB | Uso |
|---|---|---|
| `ng_core.ng_user_profiles` | `sensory_preferences` | Configurações sensoriais individuais (reduce_animations, high_contrast, etc.) |
| `ng_identity.ng_audit_logs` | `metadata` | Dados contextuais de cada ação auditada |
| `ng_audit.ng_db_audit` | `old_data`, `new_data` | Snapshots de linha antes/depois de DML |
| `ng_audit.ng_system_logs` | `metadata` | Dados adicionais de eventos de sistema |
| `ng_vault.ng_vault_entries` | _(entry_type como hint)_ | Metadados do tipo de entrada do cofre |

---

## 6. Schema `ng_identity`

> **Propósito:** autenticação, sessões, tokens de ciclo de vida de conta, consentimento e auditoria de acesso.  
> **Migration:** `V1__schema_identity.sql`  
> **Regra de segurança:** nenhuma coluna de segredo é armazenada em texto claro — apenas hashes ou ciphertext.

### Tabelas

---

#### `ng_identity.ng_users`

Tabela central do sistema. Todo usuário do Neuro-Gen possui exatamente um registro aqui.

```sql
CREATE TABLE ng_identity.ng_users (
    id             UUID        PRIMARY KEY,
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
```

| Coluna | Detalhe |
|---|---|
| `email` | Normalizado para lowercase antes do INSERT. `VARCHAR(254)` = limite máximo do RFC 5321. |
| `password_hash` | Hash Argon2id (m=19.456 KB, t=2, p=1) gerado por `Argon2PasswordHasher`. Nunca armazenado em texto claro. |
| `role` | `TITULAR` ou `APOIO`. TITULAR é o dono dos dados; APOIO é o cuidador com acesso delegado. |
| `status` | `PENDING_VERIFICATION` → `ACTIVE` → `SUSPENDED` / `DELETED`. Transições controladas por `AccountStatus`. |
| `deleted_at` | Soft-delete. Dados permanecem por período legal antes da anonimização definitiva. |

---

#### `ng_identity.ng_sessions`

Persiste refresh tokens. Access tokens (JWT) são stateless e **nunca** armazenados aqui.

```sql
CREATE TABLE ng_identity.ng_sessions (
    id                 UUID        PRIMARY KEY,
    user_id            UUID        NOT NULL REFERENCES ng_identity.ng_users (id),
    refresh_token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at         TIMESTAMPTZ NOT NULL,
    device_info        VARCHAR(512),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at         TIMESTAMPTZ
);
```

| Coluna | Detalhe |
|---|---|
| `refresh_token_hash` | SHA-256 do token opaco enviado ao cliente. O token real nunca toca o banco. |
| `device_info` | User-Agent + IP hash — para exibição na tela "sessões ativas" (futuro). |
| `revoked_at` | `NULL` = sessão ativa. `NOT NULL` = revogada (logout ou revogação por mudança de senha). Registros nunca são deletados — auditoria de segurança. |

**Por que não invalidar tokens JWT?** JWTs são stateless por definição. A revogação é feita no refresh token (stateful): ao renovar tokens, verifica-se se `revoked_at IS NULL AND expires_at > NOW()`. Access tokens vivem 15 minutos — janela de risco aceitável sem blacklist.

---

#### `ng_identity.ng_email_verifications`

Tokens de verificação de e-mail com TTL de 24 horas.

```sql
CREATE TABLE ng_identity.ng_email_verifications (
    id         UUID        PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES ng_identity.ng_users (id),
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

`used_at NOT NULL` invalida o token após uso único. O token enviado por e-mail nunca é armazenado — apenas seu SHA-256.

---

#### `ng_identity.ng_password_resets`

Tokens de redefinição de senha com TTL de 15 minutos (RF-01.7).

```sql
CREATE TABLE ng_identity.ng_password_resets (
    id         UUID        PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES ng_identity.ng_users (id),
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

Mesmo padrão de `ng_email_verifications`: hash do token, uso único, TTL por `expires_at`.

---

#### `ng_identity.ng_totp_secrets`

Segredo TOTP para autenticação de dois fatores (RF-01.9). Relação 1:1 com `ng_users`.

```sql
CREATE TABLE ng_identity.ng_totp_secrets (
    id               UUID        PRIMARY KEY,
    user_id          UUID        NOT NULL UNIQUE REFERENCES ng_identity.ng_users (id),
    encrypted_secret VARCHAR(512) NOT NULL,
    enabled_at       TIMESTAMPTZ NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

`encrypted_secret` armazena o segredo TOTP cifrado com AES-256-GCM em repouso — nunca o segredo base32 em texto claro. O trigger de auditoria em V5 usa `fn_capture_dml_redacted` para remover `encrypted_secret` antes de gravar no log.

---

#### `ng_identity.ng_consent_records`

Registro imutável de aceite de políticas de privacidade e termos de uso (RNF-PRIV.8 / LGPD art. 7º, I).

```sql
CREATE TABLE ng_identity.ng_consent_records (
    id                     UUID        PRIMARY KEY,
    user_id                UUID        NOT NULL REFERENCES ng_identity.ng_users (id),
    privacy_policy_version VARCHAR(40) NOT NULL,
    terms_version          VARCHAR(40) NOT NULL,
    ip_address             VARCHAR(64) NOT NULL,
    consented_at           TIMESTAMPTZ NOT NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

Cada aceitação de nova versão de política gera um novo registro. Sem UPDATE — append-only por design (a aplicação nunca atualiza este registro).

---

#### `ng_identity.ng_audit_logs`

Log de ações sensíveis gerado pela camada de aplicação (predecessor do `ng_audit.ng_system_logs`).

```sql
CREATE TABLE ng_identity.ng_audit_logs (
    id              UUID        PRIMARY KEY,
    user_id         UUID,
    action          VARCHAR(80) NOT NULL,
    resource_type   VARCHAR(80),
    resource_id     VARCHAR(80),
    ip_address      VARCHAR(64),
    user_agent_hash VARCHAR(128),
    metadata        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

`user_id` pode ser `NULL` para ações de sistema (ex: expiração automática de tokens). Sem `updated_at` — tabela imutável por convenção (a auditoria de banco via trigger cobre mutações em `ng_audit.ng_db_audit`).

---

## 7. Schema `ng_core`

> **Propósito:** dados operacionais do produto — vínculos de suporte, perfis, agenda, tarefas, foco, notificações.  
> **Migration:** `V2__schema_core.sql`  
> **Acesso por Apoio:** subconjunto controlado pela `PermissionMatrix` em `ng_link_permissions`.

### Tabelas

---

#### `ng_core.ng_support_links`

Vínculo entre Titular e Apoio. Cada vínculo representa uma relação de suporte autorizada.

```sql
CREATE TABLE ng_core.ng_support_links (
    id                UUID        PRIMARY KEY,
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
```

| Coluna | Detalhe |
|---|---|
| `apoio_id` | `NULL` enquanto o convite não foi aceito (status `PENDING`). Preenchido no `accept`. |
| `invite_token_hash` | SHA-256 do token de convite com TTL 72h. O token é enviado ao Apoio por e-mail/link. |
| `status` | `PENDING` → `ACTIVE` → `REVOKED`. Transições controladas por `LinkLifecycleUseCase`. |

---

#### `ng_core.ng_link_permissions`

Permissões granulares opt-in por módulo para cada vínculo. Ausência de linha = acesso negado.

```sql
CREATE TABLE ng_core.ng_link_permissions (
    id          UUID        PRIMARY KEY,
    link_id     UUID        NOT NULL REFERENCES ng_core.ng_support_links (id),
    module_name VARCHAR(40) NOT NULL,
    granted_by  UUID        NOT NULL REFERENCES ng_identity.ng_users (id),
    granted_at  TIMESTAMPTZ NOT NULL
);
```

`module_name` assume valores: `AGENDA`, `FOCUS`, `EMOTIONAL`, `VAULT`. O Cofre (`VAULT`) tem proteção estrutural adicional na camada de segurança — mesmo com permissão no banco, contas `APOIO` recebem 403 pelo filtro do Spring Security.

---

#### `ng_core.ng_user_profiles`

Perfil de preferências e configurações de acessibilidade. PK = `user_id` (relação 1:1 com `ng_users`).

```sql
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
```

| Coluna | Detalhe |
|---|---|
| `neurodivergence_profile` | Array de strings: `['TDAH', 'TEA']`, `['2E']`, etc. `NULL` ≠ array vazio. |
| `prefer_not_inform_profile` | Distinção explícita entre "não informou ainda" (`NULL`) e "prefere não informar" (`TRUE`). |
| `sensory_preferences` | JSONB livre: `{"reduce_animations": true, "high_contrast": false}`. Evolui sem migration. |

> Perfis de neurodivergência ficam aqui (ng_core) e não no ng_sensitive porque são autorreferenciados e usados para configuração de UI — não para diagnóstico médico. Dados clínicos iriam para ng_sensitive.

---

#### `ng_core.ng_categories`

Categorias visuais personalizadas criadas pelo usuário para organizar eventos e tarefas.

```sql
CREATE TABLE ng_core.ng_categories (
    id         UUID        PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES ng_identity.ng_users (id),
    name       VARCHAR(80) NOT NULL,
    color      VARCHAR(7)  NOT NULL,   -- hex #RRGGBB
    icon       VARCHAR(60),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

#### `ng_core.ng_events`

Compromissos com data e hora fixas. Suporta recorrência via RRULE (RFC 5545).

```sql
CREATE TABLE ng_core.ng_events (
    id                  UUID         PRIMARY KEY,
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
```

| Coluna | Detalhe |
|---|---|
| `rrule` | String RFC 5545. Ex: `FREQ=WEEKLY;BYDAY=MO,WE,FR`. `NULL` = evento único. |
| `shared_with_support` | Controle de visibilidade para Apoios com permissão `AGENDA`. |
| `cancelled_at` | Soft-delete. Eventos cancelados permanecem no histórico. |

---

#### `ng_core.ng_tasks`

Tarefas sem horário fixo. Diferente de eventos, não têm `start_at`/`end_at` — apenas `due_date` opcional.

```sql
CREATE TABLE ng_core.ng_tasks (
    id                  UUID         PRIMARY KEY,
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
```

`status`: `PENDING` → `IN_PROGRESS` → `COMPLETED` → `ARCHIVED`. Transições validadas no domínio por `TaskStatus`.  
`priority`: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`.

---

#### `ng_core.ng_subtasks`

Subtarefas filhas de uma tarefa. Ordenadas por `position` para drag-and-drop estável.

```sql
CREATE TABLE ng_core.ng_subtasks (
    id           UUID         PRIMARY KEY,
    task_id      UUID         NOT NULL REFERENCES ng_core.ng_tasks (id),
    title        VARCHAR(200) NOT NULL,
    done         BOOLEAN      NOT NULL DEFAULT FALSE,
    position     INTEGER      NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

---

#### `ng_core.ng_reminders`

Lembretes vinculados a eventos **ou** tarefas — nunca aos dois simultaneamente.

```sql
CREATE TABLE ng_core.ng_reminders (
    id                  UUID        PRIMARY KEY,
    event_id            UUID        REFERENCES ng_core.ng_events (id),
    task_id             UUID        REFERENCES ng_core.ng_tasks (id),
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
        (event_id IS NOT NULL AND task_id IS NULL) OR
        (event_id IS NULL    AND task_id IS NOT NULL)
    )
);
```

A `CHECK CONSTRAINT` `ng_reminders_event_xor_task` garante **exclusividade mútua** entre `event_id` e `task_id` diretamente no banco — a invariante não depende de validação na aplicação.

| Coluna | Detalhe |
|---|---|
| `persistent` | Se `TRUE`, o lembrete se repete até confirmação explícita (comportamento nudge — RF-03.2). |
| `repeat_interval_min` | Intervalo de repetição em minutos para lembretes persistentes. |
| `max_repetitions` / `current_repetitions` | Limite de repetições antes de expirar (evita spam). |
| `status` | `SCHEDULED` → `SENT` → `CONFIRMED` / `EXPIRED` / `UNCONFIRMED_LIMIT_REACHED`. |

---

#### `ng_core.ng_focus_sessions`

Sessões de foco Pomodoro com suporte a pausas múltiplas.

```sql
CREATE TABLE ng_core.ng_focus_sessions (
    id                UUID        PRIMARY KEY,
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
```

`effective_seconds` = tempo total da sessão menos o tempo em pausas. Calculado pela aplicação e mantido atualizado a cada `resume`.

---

#### `ng_core.ng_focus_pauses`

Registro granular de cada pausa de uma sessão de foco.

```sql
CREATE TABLE ng_core.ng_focus_pauses (
    id         UUID        PRIMARY KEY,
    session_id UUID        NOT NULL REFERENCES ng_core.ng_focus_sessions (id),
    paused_at  TIMESTAMPTZ NOT NULL,
    resumed_at TIMESTAMPTZ
);
```

`resumed_at NULL` = pausa ainda ativa. Permite calcular o tempo total pausado sem depender de agregações em `ng_focus_sessions`.

---

#### `ng_core.ng_notification_channels`

Canais de entrega de notificações configurados por usuário.

```sql
CREATE TABLE ng_core.ng_notification_channels (
    id            UUID        PRIMARY KEY,
    user_id       UUID        NOT NULL REFERENCES ng_identity.ng_users (id),
    channel_type  VARCHAR(20) NOT NULL,   -- PUSH | EMAIL | SMS
    endpoint      VARCHAR(512) NOT NULL,
    active        BOOLEAN     NOT NULL DEFAULT TRUE,
    silence_start TIME,
    silence_end   TIME,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

`silence_start`/`silence_end` define a janela de silêncio configurável pelo usuário (RF-03.8). `TIME` sem timezone — interpretado no fuso do usuário (`ng_user_profiles.timezone`).

---

#### `ng_core.ng_notification_logs`

Histórico de notificações enviadas (últimos 30 dias disponíveis na Central — RF-03.7).

```sql
CREATE TABLE ng_core.ng_notification_logs (
    id             UUID        PRIMARY KEY,
    user_id        UUID        NOT NULL REFERENCES ng_identity.ng_users (id),
    reminder_id    UUID        REFERENCES ng_core.ng_reminders (id),
    channel_type   VARCHAR(20) NOT NULL,
    status         VARCHAR(20) NOT NULL,
    sent_at        TIMESTAMPTZ,
    delivered_at   TIMESTAMPTZ,
    confirmed_at   TIMESTAMPTZ,
    failure_reason VARCHAR(255),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

## 8. Schema `ng_sensitive`

> **Propósito:** dados de saúde mental — LGPD art. 5º, II (dado sensível).  
> **Migration:** `V3__schema_sensitive.sql`  
> **Proteção:** colunas de conteúdo cifradas em repouso com AES-256-GCM pela aplicação antes do INSERT.  
> **Acesso por Apoio:** exige `ConsentRecord` ativo + permissão `EMOTIONAL` no vínculo + verificação dupla (ADR-006).

**Por schema separado?** A LGPD distingue "dado pessoal" de "dado sensível de saúde". Manter um schema separado permite:
- Grants de banco diferentes (`REVOKE SELECT ON SCHEMA ng_sensitive FROM app_readonly_role`)
- Auditoria e políticas de retenção distintas
- Futuro particionamento ou criptografia transparente de tablespace

---

#### `ng_sensitive.ng_mood_checkins`

Check-ins diários de humor e energia do usuário.

```sql
CREATE TABLE ng_sensitive.ng_mood_checkins (
    id           UUID        PRIMARY KEY,
    user_id      UUID        NOT NULL REFERENCES ng_identity.ng_users (id),
    mood_score   SMALLINT    NOT NULL,   -- 1 a 10
    energy_level SMALLINT    NOT NULL,   -- 1 a 10
    notes_enc    BYTEA,                  -- cifrado AES-256-GCM, pode ser NULL
    checkin_date DATE        NOT NULL,
    checkin_time TIME        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

`notes_enc` armazena o ciphertext AES-256-GCM. Mesmo que o banco seja comprometido, sem a `SENSITIVE_DATA_KEY` o conteúdo é ininteligível. `BYTEA` é preferível a `TEXT` para dados binários — evita conversão de charset.

---

#### `ng_sensitive.ng_journal_entries`

Entradas de diário emocional. Conteúdo completamente cifrado.

```sql
CREATE TABLE ng_sensitive.ng_journal_entries (
    id                 UUID        PRIMARY KEY,
    user_id            UUID        NOT NULL REFERENCES ng_identity.ng_users (id),
    content_enc        BYTEA       NOT NULL,
    related_checkin_id UUID        REFERENCES ng_sensitive.ng_mood_checkins (id),
    entry_date         DATE        NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

`content_enc NOT NULL` — não existe entrada de diário sem conteúdo. Nunca indexado para full-text search — PII de saúde não pode ser analisada pelo banco.

---

#### `ng_sensitive.ng_neurodivergence_profiles`

Perfil de neurodivergência declarado pelo próprio usuário. Relação 1:1.

```sql
CREATE TABLE ng_sensitive.ng_neurodivergence_profiles (
    id                UUID        PRIMARY KEY,
    user_id           UUID        NOT NULL UNIQUE REFERENCES ng_identity.ng_users (id),
    declared_profiles VARCHAR(40)[],
    prefer_not_inform BOOLEAN     NOT NULL DEFAULT FALSE,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

`declared_profiles NULL` ≠ `prefer_not_inform = TRUE` ≠ array vazio — cada estado tem semântica distinta. Nunca restringe funcionalidades — usado apenas para sugerir configurações iniciais de acessibilidade (RF-10).

---

## 9. Schema `ng_audit`

> **Propósito:** auditoria imutável de operações de banco e eventos de sistema.  
> **Migration:** `V5__schema_audit.sql`  
> **Requisitos:** RNF-SEC.8 / RNF-PRIV.8 / LGPD art. 37  
> **Retenção mínima:** 12 meses

### Imutabilidade garantida em nível de banco

A função `ng_audit.fn_block_audit_mutations()` está instalada como trigger `BEFORE UPDATE OR DELETE` em ambas as tabelas. Qualquer tentativa de modificar ou apagar um registro de auditoria lança:

```
ERROR: Operação proibida: a tabela de auditoria "ng_db_audit" é imutável.
UPDATE e DELETE são bloqueados em nível de banco (RNF-SEC.8).
SQLSTATE: 55000
```

**Isso vale inclusive para o superusuário do banco em sessões normais.** O único bypass seria desativar o trigger (`ALTER TABLE ... DISABLE TRIGGER`), operação que requer `SUPERUSER` e ficaria registrada no log do PostgreSQL.

---

#### `ng_audit.ng_db_audit`

Registra automaticamente toda operação DML nas tabelas críticas. **Alimentada por triggers PostgreSQL — zero código Java.**

```sql
CREATE TABLE ng_audit.ng_db_audit (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    occurred_at TIMESTAMPTZ NOT NULL    DEFAULT NOW(),
    operation   VARCHAR(10) NOT NULL,         -- INSERT | UPDATE | DELETE
    schema_name VARCHAR(63) NOT NULL,
    table_name  VARCHAR(63) NOT NULL,
    record_id   TEXT,
    old_data    JSONB,                         -- snapshot antes (UPDATE/DELETE)
    new_data    JSONB,                         -- snapshot depois (INSERT/UPDATE)
    db_user     VARCHAR(128) NOT NULL DEFAULT current_user,
    app_user_id UUID,
    client_addr INET,
    tx_id       BIGINT      NOT NULL DEFAULT txid_current(),
    CONSTRAINT ng_db_audit_op_check CHECK (operation IN ('INSERT','UPDATE','DELETE'))
);
```

**Duas funções de captura:**

| Função | Usada em | Comportamento |
|---|---|---|
| `fn_capture_dml()` | Tabelas sem dados sensíveis | Grava `old_data` e `new_data` completos como JSONB |
| `fn_capture_dml_redacted()` | Tabelas com credenciais | Remove `password_hash`, `refresh_token_hash`, `token_hash`, `encrypted_secret` antes de gravar |

**Tabelas auditadas:**

| Schema | Tabela | Função |
|---|---|---|
| `ng_identity` | `ng_users` | `fn_capture_dml_redacted` |
| `ng_identity` | `ng_sessions` | `fn_capture_dml_redacted` |
| `ng_identity` | `ng_totp_secrets` | `fn_capture_dml_redacted` |
| `ng_identity` | `ng_password_resets` | `fn_capture_dml_redacted` |
| `ng_identity` | `ng_consent_records` | `fn_capture_dml` |
| `ng_identity` | `ng_audit_logs` | `fn_capture_dml` |
| `ng_core` | `ng_support_links` | `fn_capture_dml` |
| `ng_core` | `ng_link_permissions` | `fn_capture_dml` |
| `ng_core` | `ng_events` | `fn_capture_dml` |
| `ng_core` | `ng_tasks` | `fn_capture_dml` |
| `ng_core` | `ng_focus_sessions` | `fn_capture_dml` |
| `ng_sensitive` | `ng_mood_checkins` | `fn_capture_dml` |

---

#### `ng_audit.ng_system_logs`

Eventos gerados pela camada de aplicação Java. Escrita assíncrona via `AuditService` (`@Async`).

```sql
CREATE TABLE ng_audit.ng_system_logs (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    logged_at    TIMESTAMPTZ NOT NULL    DEFAULT NOW(),
    level        VARCHAR(10) NOT NULL,   -- TRACE|DEBUG|INFO|WARN|ERROR|FATAL
    logger_name  VARCHAR(255),
    message      TEXT        NOT NULL,
    stack_trace  TEXT,
    trace_id     VARCHAR(64),
    span_id      VARCHAR(32),
    user_id      UUID,
    request_path VARCHAR(512),
    http_method  VARCHAR(10),
    http_status  SMALLINT,
    duration_ms  INTEGER,
    ip_address   VARCHAR(64),
    user_agent   VARCHAR(512),
    action       VARCHAR(80),
    resource_type VARCHAR(80),
    resource_id  TEXT,
    metadata     JSONB,
    CONSTRAINT ng_system_logs_level_check CHECK (
        level IN ('TRACE','DEBUG','INFO','WARN','ERROR','FATAL')
    )
);
```

**Diferença entre `ng_db_audit` e `ng_system_logs`:**

| Aspecto | `ng_db_audit` | `ng_system_logs` |
|---|---|---|
| Alimentado por | Triggers PostgreSQL | Java (`AuditService.@Async`) |
| Timing | Síncrono (mesma transação) | Assíncrono (thread separada) |
| Escopo | DML nas tabelas | Eventos de domínio, HTTP, segurança |
| Falha ao persistir | Impossível (mesma transação) | Não reverte a operação original |
| Cobertura | Acesso direto ao banco também | Apenas via aplicação |

> Para referência completa de queries de auditoria, consulte `docs/integracao/api-audit.md`.

---

## 10. Schema `ng_vault` (banco separado)

> **Propósito:** cofre zero-knowledge de credenciais e segredos do usuário Titular.  
> **Migration:** `src/main/resources/db/vault/migration/V1__schema_vault.sql`  
> **Isolamento:** DataSource e instância PostgreSQL completamente separados em produção (ADR-005).  
> **Garantia zero-knowledge:** o servidor nunca vê o conteúdo decifrado. Cifração ocorre no cliente (ou na aplicação com chave que nunca toca o banco).

### Por que banco separado e não apenas schema separado?

| Risco | Schema separado | Banco separado |
|---|---|---|
| DBA com acesso ao banco vê entradas do cofre | Sim (precisa de REVOKE explícito) | Não (credenciais diferentes) |
| Bug em migration do Core dropa schema do Vault | Possível (mesma conexão) | Impossível (conexões isoladas) |
| Comprometimento das credenciais do Core expõe o Vault | Sim | Não |
| Vazamento de dump do Core expõe o Vault | Sim | Não |

---

#### `ng_vault.ng_vault_entries`

Entradas do cofre. O servidor armazena apenas bytes cifrados.

```sql
CREATE TABLE ng_vault.ng_vault_entries (
    id         UUID         PRIMARY KEY,
    owner_id   UUID         NOT NULL,       -- sem FK cross-DB (isolamento)
    label      VARCHAR(200) NOT NULL,
    entry_type VARCHAR(30)  NOT NULL,        -- LOGIN|NOTE|CARD|API_KEY|CUSTOM
    ciphertext BYTEA        NOT NULL,
    iv         BYTEA        NOT NULL,        -- nonce AES-256-GCM (16 bytes)
    salt       BYTEA        NOT NULL,        -- salt para derivação Argon2id
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

**Por que sem FK para `ng_identity.ng_users`?** O banco do Vault é uma instância separada — não há como referenciar tabelas de outro banco com FK relacional. `owner_id` é o UUID do Titular, mantido consistente pela aplicação.

**Componentes criptográficos:**

| Coluna | Propósito |
|---|---|
| `ciphertext` | Conteúdo cifrado AES-256-GCM. Ininteligível sem a chave derivada. |
| `iv` | Nonce único por entrada (16 bytes). Nunca reutilizado — garante que o mesmo plaintext produza ciphertexts diferentes. |
| `salt` | Salt Argon2id para derivação da chave mestra. Único por entrada — permite troca de senha mestra sem re-cifrar todas as entradas. |

---

#### `ng_vault.ng_vault_auth_logs`

Log de tentativas de acesso ao Cofre — incluindo negações de contas APOIO.

```sql
CREATE TABLE ng_vault.ng_vault_auth_logs (
    id              UUID        PRIMARY KEY,
    user_id         UUID        NOT NULL,
    action          VARCHAR(40) NOT NULL,
    ip_address      VARCHAR(64),
    user_agent_hash VARCHAR(128),
    success         BOOLEAN     NOT NULL,
    failure_reason  VARCHAR(255),
    attempted_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

Imutável por convenção (a aplicação nunca atualiza este registro). `user_agent_hash` = SHA-256 do User-Agent — não é PII direta, mas permite correlação de sessões.

---

#### `ng_vault.ng_master_key_meta`

Parâmetros para derivação da chave mestra. **Nunca armazena a chave ou a senha mestra.**

```sql
CREATE TABLE ng_vault.ng_master_key_meta (
    id                   UUID         PRIMARY KEY,
    user_id              UUID         NOT NULL UNIQUE,
    argon2_params        VARCHAR(100) NOT NULL,
    master_verifier_hash VARCHAR(128) NOT NULL,
    recovery_key_hash    VARCHAR(128) NOT NULL,
    configured_at        TIMESTAMPTZ  NOT NULL,
    last_auth_at         TIMESTAMPTZ
);
```

| Coluna | Detalhe |
|---|---|
| `argon2_params` | Parâmetros Argon2id: `m=19456,t=2,p=1`. Permite upgrades futuros de parâmetros sem re-derivação imediata. |
| `master_verifier_hash` | Hash derivado da senha mestra. Permite verificar se a senha informada está correta sem armazená-la. |
| `recovery_key_hash` | SHA-256 da chave de recuperação gerada no setup. Permite reset do cofre se a senha mestra for esquecida (RF-08.11). |

---

## 11. Estratégia de índices

> **Referência:** `V4__indexes.sql`  
> **Meta de performance:** suportar 100.000 MAU sem redesenho de schema (RNF-ESC.4).

### Princípios de indexação

1. **Índice apenas onde há query conhecida** — não criar índices preventivos em colunas não usadas em WHERE/JOIN/ORDER BY
2. **Compostos quando a query sempre filtra pelos dois campos juntos** — `(owner_id, status, due_date)` para "minhas tarefas pendentes com prazo"
3. **Índice UNIQUE na coluna de negócio** — `email`, `refresh_token_hash`, `invite_token_hash` têm UNIQUE constraint que implicitamente cria índice
4. **Partial index futuro** — para `revoked_at IS NULL` em `ng_sessions` (sessões ativas), candidato a índice parcial quando volume crescer

### Índices definidos

```sql
-- Sessões: buscar sessões ativas de um usuário; limpeza de sessões expiradas
CREATE INDEX idx_sessions_user_revoked  ON ng_identity.ng_sessions (user_id, revoked_at);
CREATE INDEX idx_sessions_expires       ON ng_identity.ng_sessions (expires_at);

-- Vínculos: vínculos ativos por Titular e por Apoio
CREATE INDEX idx_links_titular_status   ON ng_core.ng_support_links (titular_id, status);
CREATE INDEX idx_links_apoio_status     ON ng_core.ng_support_links (apoio_id, status);
CREATE INDEX idx_link_perms_link        ON ng_core.ng_link_permissions (link_id);

-- Agenda: visão de calendário por período; tarefas ativas por data; subtarefas ordenadas
CREATE INDEX idx_events_owner_period    ON ng_core.ng_events (owner_id, start_at, end_at);
CREATE INDEX idx_tasks_owner_status_due ON ng_core.ng_tasks (owner_id, status, due_date);
CREATE INDEX idx_subtasks_task_position ON ng_core.ng_subtasks (task_id, position);

-- Lembretes: polling do Notification Service (próximos lembretes a disparar)
CREATE INDEX idx_reminders_status_fire  ON ng_core.ng_reminders (status, next_fire_at);

-- Foco: sessão ativa do usuário
CREATE INDEX idx_focus_user_status      ON ng_core.ng_focus_sessions (user_id, status);

-- Sensitivo: dashboard de tendência de humor (série temporal por usuário)
CREATE INDEX idx_mood_user_date         ON ng_sensitive.ng_mood_checkins (user_id, checkin_date DESC);
```

### Índices de auditoria (V5)

```sql
-- ng_db_audit: consultas forenses por tabela, por usuário, por registro, por transação
CREATE INDEX idx_db_audit_table_time ON ng_audit.ng_db_audit (schema_name, table_name, occurred_at DESC);
CREATE INDEX idx_db_audit_app_user   ON ng_audit.ng_db_audit (app_user_id, occurred_at DESC);
CREATE INDEX idx_db_audit_record     ON ng_audit.ng_db_audit (table_name, record_id);
CREATE INDEX idx_db_audit_tx         ON ng_audit.ng_db_audit (tx_id);

-- ng_system_logs: por tempo, por nível, por usuário, por ação, por trace_id, por status HTTP
CREATE INDEX idx_sys_logs_time       ON ng_audit.ng_system_logs (logged_at DESC);
CREATE INDEX idx_sys_logs_level_time ON ng_audit.ng_system_logs (level, logged_at DESC);
CREATE INDEX idx_sys_logs_user       ON ng_audit.ng_system_logs (user_id, logged_at DESC);
CREATE INDEX idx_sys_logs_action     ON ng_audit.ng_system_logs (action, logged_at DESC);
CREATE INDEX idx_sys_logs_trace      ON ng_audit.ng_system_logs (trace_id);
CREATE INDEX idx_sys_logs_status     ON ng_audit.ng_system_logs (http_status, logged_at DESC);
```

### Índices do Vault (V1 vault)

```sql
CREATE INDEX idx_vault_entries_owner ON ng_vault.ng_vault_entries (owner_id);
CREATE INDEX idx_vault_auth_user     ON ng_vault.ng_vault_auth_logs (user_id, attempted_at DESC);
```

---

## 12. Tipos de dado e justificativas

| Tipo PostgreSQL | Usado para | Justificativa |
|---|---|---|
| `UUID` | Todas as PKs e FKs | Sem sequência previsível (segurança); gerado na aplicação (domain events antes do persist) |
| `TIMESTAMPTZ` | Todos os timestamps | Armazena instante absoluto em UTC; consistente independente do fuso do servidor |
| `DATE` | `due_date`, `checkin_date`, `entry_date` | Data sem hora — fuso do usuário resolve na UI |
| `TIME` | `checkin_time`, `silence_start`, `silence_end` | Hora local do usuário; interpretada junto com `timezone` do perfil |
| `VARCHAR(n)` | Strings com limite conhecido | Validação de tamanho no banco além da validação do Bean Validation |
| `TEXT` | Conteúdo sem limite previsível | `description`, `stack_trace`, `message` — sem truncamento silencioso |
| `BYTEA` | Dados binários cifrados | `notes_enc`, `content_enc`, `ciphertext`, `iv`, `salt` — evita conversão de charset |
| `JSONB` | Dados semi-estruturados | `metadata`, `sensory_preferences`, `old_data`, `new_data` — indexável, operadores nativos |
| `SMALLINT` | Scores (1–10) | Menor footprint que `INTEGER` para valores em faixa estreita |
| `BIGINT` | `effective_seconds`, `tx_id` | Duração em segundos pode ultrapassar `INTEGER`; `txid_current()` retorna bigint |
| `BOOLEAN` | Flags binárias | Sem alternativa VARCHAR — evita strings "true"/"false"/"1"/"0" |
| `INET` | Endereços IP | Tipo nativo PostgreSQL com operadores CIDR/subnet; armazena IPv4 e IPv6 |
| `VARCHAR(40)[]` | Arrays de enums | `neurodivergence_profile`, `declared_profiles` — array nativo sem tabela de junção |

---

## 13. Decisões de design registradas

### ADR-005 — DataSource isolado para o Vault

**Decisão:** o schema `ng_vault` usa uma instância PostgreSQL separada em produção, com credentials e HikariCP pool próprios.

**Contexto:** o Vault armazena credenciais pessoais cifradas do usuário. Um comprometimento do banco Core (SSRF, SQL injection, dump de backup) não pode expor o Vault.

**Consequência:** sem FK relacional entre `ng_vault.ng_vault_entries.owner_id` e `ng_identity.ng_users.id` — integridade garantida pela aplicação. Complexidade de operação aumenta (dois bancos para backup, monitoramento, etc.).

---

### ADR-006 — AES-256-GCM em `ng_sensitive`

**Decisão:** colunas de conteúdo em `ng_sensitive` são cifradas na camada de aplicação antes do INSERT, armazenadas como `BYTEA`.

**Contexto:** `ng_mood_checkins.notes_enc` e `ng_journal_entries.content_enc` são dados sensíveis de saúde (LGPD art. 5º, II). Criptografia transparente de disco (TDE) não é suficiente — protege apenas contra roubo de arquivo físico. AES-256-GCM na aplicação protege também contra acesso direto ao banco por um DBA mal-intencionado.

**Chave:** `neurogen.security.sensitive-data-key` (hex 64 chars). Em produção: variável de ambiente `SENSITIVE_DATA_KEY`. Nunca no código ou no banco.

**Consequência:** sem full-text search nessas colunas. Busca por conteúdo requer decifrar na aplicação e filtrar em memória — aceitável dado o volume por usuário.

---

### Decisão — UUIDs gerados pela aplicação (não pelo banco)

**Decisão:** PKs como `UUID PRIMARY KEY` sem `DEFAULT gen_random_uuid()` na maioria das tabelas.

**Contexto:** o padrão hexagonal exige que o domínio possa construir um `AggregateRoot` completo — incluindo seu ID — antes de qualquer I/O. Sem isso, `AggregateRoot.registerEvent(new UserRegistered(this.id))` precisaria esperar o INSERT para conhecer o ID.

**Consequência:** a aplicação chama `UUID.randomUUID()` antes de cada INSERT. Overhead negligenciável. O banco ainda valida unicidade via PRIMARY KEY.

---

### Decisão — Sem `ENUM` nativo do PostgreSQL

**Decisão:** todos os campos de tipo enumerado usam `VARCHAR` com `CHECK CONSTRAINT` onde crítico.

**Contexto:** `ALTER TYPE ... ADD VALUE` em enum nativo do PostgreSQL não é transacional (antes do PG 12) e ainda adquire lock breve em versões modernas. Em produção sem downtime, isso é um risco.

**Consequência:** sem type-safety nativa no banco para enums. A validação é responsabilidade do Bean Validation e do domínio Java. `CHECK CONSTRAINT` é adicionada nos campos de auditoria por criticidade.

---

### Decisão — `TIMESTAMPTZ` em todos os timestamps

**Decisão:** sem `TIMESTAMP WITHOUT TIME ZONE` em nenhuma tabela.

**Contexto:** o PostgreSQL armazena `TIMESTAMPTZ` como UTC internamente e converte no display. Com `TIMESTAMP` (sem fuso), valores dependem do `TimeZone` do servidor — um reconfigure de sistema operacional ou migração de servidor pode corromper a interpretação de datas históricas.

**Consequência:** o Hibernate é configurado com `hibernate.jdbc.time_zone: UTC`. Todos os `Instant`/`ZonedDateTime` Java chegam ao banco como UTC.

---

### Decisão — Soft-delete seletivo, não universal

**Decisão:** soft-delete apenas onde há obrigação legal ou necessidade de histórico — não como padrão universal.

**Contexto:** soft-delete universal cria overhead em todas as queries (WHERE deleted_at IS NULL), polui o banco com dados "fantasma" e complica a análise de dados.

**Regras aplicadas:**
- `ng_users`: soft-delete (LGPD — período de retenção antes de anonimização)
- `ng_sessions`: revogação, nunca deleção (histórico de segurança)
- `ng_events`, `ng_tasks`: soft-delete (histórico de atividade)
- `ng_vault_entries`: hard-delete (RNF-RET.2 — dado do Vault eliminado irreversivelmente ao excluir conta)
- Tabelas de tokens (`ng_email_verifications`, `ng_password_resets`): hard-delete após expiração pode ser feito por job de limpeza

---

### Decisão — `CHECK CONSTRAINT` para exclusividade mútua em `ng_reminders`

**Decisão:** restrição XOR entre `event_id` e `task_id` implementada no banco, não só na aplicação.

**Contexto:** um lembrete deve pertencer a exatamente um contexto (evento OU tarefa). Validar apenas na aplicação deixa a invariante vulnerável a migrations diretas, importações de dados e bugs de concorrência.

**Implementação:**
```sql
CONSTRAINT ng_reminders_event_xor_task CHECK (
    (event_id IS NOT NULL AND task_id IS NULL) OR
    (event_id IS NULL    AND task_id IS NOT NULL)
)
```

---

## 14. Referências cruzadas

| Documento | Descrição |
|---|---|
| `docs/diagramas/02-der.md` | DER visual (Mermaid) de todos os schemas |
| `docs/diagramas/der/01-schema-identity.puml` | DER PlantUML — `ng_identity` |
| `docs/diagramas/der/02-schema-core.puml` | DER PlantUML — `ng_core` |
| `docs/diagramas/der/03-schema-sensitive.puml` | DER PlantUML — `ng_sensitive` |
| `docs/diagramas/der/04-schema-vault.puml` | DER PlantUML — `ng_vault` |
| `docs/diagramas/der/05-schema-audit.puml` | DER PlantUML — `ng_audit` |
| `docs/integracao/api-audit.md` | Queries de consulta forense e referência da auditoria |
| `docs/defContexto/03-requisitos-nao-funcionais.md` | RNF-SEC.*, RNF-PRIV.*, RNF-ESC.* mencionados neste documento |
| `docs/defContexto/05-adrs.md` | ADRs completos (ADR-005, ADR-006, ADR-007, ADR-008) |
| `src/main/resources/db/migration/` | Fonte de verdade do schema Core |
| `src/main/resources/db/vault/migration/` | Fonte de verdade do schema Vault |
