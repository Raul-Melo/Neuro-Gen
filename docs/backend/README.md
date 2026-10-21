# Neuro-Gen — Backend (Monólito Modular + Vault isolado)

Implementação do backend seguindo **Clean/Hexagonal Architecture, SOLID, DDD e TDD**, organizada
como **monólito modular** (Java 21 + Spring Boot 3.3.x + PostgreSQL + Flyway), conforme os ADRs em
`docs/defContexto/05-adrs.md` e os diagramas em `docs/diagramas/`.

## Arquitetura por módulo

Cada módulo é um *bounded context* com quatro camadas e dependências apontando para dentro
(`domain` não conhece Spring/JPA):

```
<módulo>/
  domain/         aggregates, entities, value objects, enums, domain events, exceptions, ports
  application/    use cases (serviços), commands, results
  api/            controllers REST + Request DTOs (Bean Validation) + anotações OpenAPI
  infrastructure/ entidades JPA, adapters (implementam as ports), serviços técnicos, config
```

Regra de isolamento (ADR-001): **nenhum módulo importa classes internas de outro** — apenas do
`shared`. A integração inter-módulo usa **domain events** (`ApplicationEventPublisher`) ou portas
publicadas no `shared` (ex.: `SupportAccessPolicy`). Exemplo: ao revogar um vínculo, o Link publica
`LinkRevoked`; o Identity reage invalidando as sessões do Apoio (RF-01.4).

## Módulos

| Módulo | Responsabilidade | Base path |
|--------|------------------|-----------|
| `shared` | Kernel: `AggregateRoot`, VOs (`UserId`, `Email`, `AuditMetadata`), hierarquia de exceções, `GlobalExceptionHandler` (RFC 7807), segurança (Authorization Server + Resource Server JWT), OpenAPI, cifra AES-256-GCM | — |
| `identity` | Contas, login/JWT, refresh tokens, verificação de e-mail, reset de senha, 2FA TOTP | `/api/v1/auth`, `/api/v1/users` |
| `link` | Vínculo Titular/Apoio, convites (TTL 72h), permissões granulares opt-in (sem VAULT) | `/api/v1/links` |
| `agenda` | Eventos (hora fixa, recorrência RRULE) e Tarefas (subtarefas, lembretes) | `/api/v1/events`, `/api/v1/tasks` |
| `focus` | Sessões de foco (Pomodoro configurável), pausar/retomar, histórico | `/api/v1/focus` |
| `emotional` | Check-in de humor/energia e diário (dado sensível LGPD, cifra de coluna) + tendência | `/api/v1/emotional` |
| `vault` | Cofre zero-knowledge, **datasource/schema isolados** (ADR-005), negação estrutural de APOIO (RF-08.5) | `/api/v1/vault` |

## Segurança

- **JWT** emitido pela infraestrutura do Spring Authorization Server (JWKSource/Nimbus). Access token
  curto (15 min) + refresh token opaco persistido em `ng_sessions` (30 dias).
- Três cadeias de filtro (`shared/config/SecurityConfig`): Authorization Server, **Vault** (nega
  `ROLE_APOIO` estruturalmente e audita a tentativa em `ng_vault_auth_logs`) e API geral.
- Senha com **Argon2id** (OWASP, RNF-SEC.5). Segredo TOTP e colunas sensíveis cifrados em
  **AES-256-GCM** (RNF-SEC.2/SEC.3).

## Persistência

- **PostgreSQL** com dois datasources: Core (`ng_identity`, `ng_core`, `ng_sensitive`) e Vault
  (`ng_vault`, isolado — ADR-005). Schema versionado **apenas por Flyway** (`ddl-auto=none`).
- Migrations: `src/main/resources/db/migration/` (core V1–V4) e `db/vault/migration/` (vault V1).

## Como rodar

```bash
./mvnw spring-boot:run      # Docker Compose sobe o postgres automaticamente (compose.yaml)
```

- Swagger UI: `http://localhost:8080/swagger-ui.html` (documentação OpenAPI agrupada por módulo).
- Health: `http://localhost:8080/actuator/health`.

## Testes

```bash
./mvnw test                 # unitários (domínio + use cases) e slices de API — sem necessidade de Docker
```

- **Domínio** (JUnit5 + AssertJ): VOs e invariantes de agregado.
- **Aplicação** (Mockito): use cases, incluindo caminhos de erro.
- **API** (`MockMvc` standalone): contrato HTTP + `GlobalExceptionHandler`.
- **Integração** (`NeuroGenIntegrationTest`, Testcontainers PostgreSQL — pulado sem Docker): sobe o
  contexto com os 2 datasources + Flyway e valida o **teste de segurança obrigatório** de que uma
  conta APOIO recebe **403** no Cofre e a tentativa é auditada (RF-08.5).

## Fluxo de valor (smoke)

`register → verify-email → login (JWT) → criar convite → aceitar (Apoio) → conceder permissões →
criar evento/tarefa → sessão de foco → check-in emocional → setup/open do Cofre`.

## Divergências conscientes

- **Vault**: implementado como módulo com datasource/schema isolados no mesmo deployable (isolamento
  lógico), não como serviço físico separado do ADR-005 — registrado para evolução.
- `ng_master_key_meta` ganhou a coluna `master_verifier_hash` (verificador da senha mestra derivada
  no cliente) para validar a abertura do Cofre sem armazenar a chave (preserva o zero-knowledge).
- Itens *Should/Could* de alguns épicos (ex.: fila de notificações, sync offline) ficam como extensão
  pós-build; o foco foi o conjunto **Must** do MVP.
