# Diagramas de Classes — Interações entre Componentes

> **Checkpoint 3 · item 4.9.** Os diagramas de `01-diagrama-classes.md` mostram **o que existe dentro de
> cada módulo**. Este documento mostra **como os componentes conversam entre si** nas interações
> principais do sistema. Todos os nomes de classes e métodos correspondem ao código em `src/main/java`.
>
> **Como ler:** `--|>` herança · `..|>` implementa interface · `-->` dependência injetada (campo) ·
> `..>` usa/cria/lança · `<<interface>>` porta da arquitetura hexagonal.

| # | Interação | O que demonstra | Rastreabilidade |
|---|-----------|-----------------|-----------------|
| 1 | Requisição REST atravessando as camadas | Arquitetura hexagonal: o domínio não depende de Spring/JPA | ADR-001, RNF-MAN |
| 2 | Autenticação e autorização com JWT | Emissão (Authorization Server) e validação (Resource Server) | RF-01, RNF-SEC.5/7 |
| 3 | Integração entre módulos | Eventos de domínio e porta compartilhada, sem import entre módulos | ADR-001, RF-01.3/01.4 |
| 4 | Isolamento do Cofre | Negação estrutural do APOIO, datasource próprio, auditoria | ADR-005, RF-08.5 |
| 5 | Tratamento de erros | Exceções de domínio e do Spring traduzidas em respostas HTTP padronizadas | RNF-OBS.1 |

---

## 1. Requisição REST atravessando as camadas (exemplo: concluir uma Tarefa)

Fluxo de `POST /api/v1/tasks/{id}/complete`. O controller só traduz HTTP; o caso de uso orquestra; a regra
de negócio (máquina de estados) mora no agregado `Task`; a persistência fica atrás da porta
`TaskRepository`, implementada na infraestrutura. Trocar JPA por outra tecnologia não altera domínio nem
aplicação — basta outro adaptador.

```mermaid
classDiagram
    direction LR

    class TaskController {
        <<RestController>>
        -TaskUseCase tasks
        +create(CreateTaskRequest) ResponseEntity
        +list(String status) List~TaskResult~
        +complete(String id) TaskResult
        +archive(String id) TaskResult
        +reopen(String id) TaskResult
    }
    class CurrentUser {
        <<utility>>
        +requireId()$ UserId
    }
    class TaskUseCase {
        <<Service>>
        -TaskRepository tasks
        +create(CreateTaskCommand) TaskResult
        +complete(String taskId, String ownerId) TaskResult
        +list(String ownerId, String status) List~TaskResult~
        -load(String taskId, String ownerId) Task
        -parseStatus(String status)$ TaskStatus
    }
    class TaskRepository {
        <<interface>>
        +save(Task) Task
        +findById(TaskId) Optional~Task~
        +findByOwner(UserId) List~Task~
        +findByOwnerAndStatus(UserId, TaskStatus) List~Task~
        +delete(TaskId) void
    }
    class Task {
        <<aggregate>>
        -TaskStatus status
        +create(...)$ Task
        +complete() void
        +archive() void
        +reopen() void
        -transition(Set~TaskStatus~, TaskStatus, String) void
    }
    class AggregateRoot {
        <<abstract>>
        #registerEvent(DomainEvent) void
        +pullDomainEvents() List~DomainEvent~
    }
    class AgendaTitle {
        +validate(String)$ String
    }
    class InvalidTaskTransitionException {
        HTTP 409
    }
    class TaskRepositoryAdapter {
        <<Component>>
        +save(Task) Task
        -toDomain(TaskEntity) Task
    }
    class TaskJpaRepository {
        <<interface>>
        Spring Data JPA
    }
    class TaskEntity {
        <<Entity>>
        schema ng_core
    }
    class TaskResult {
        <<record>>
        +from(Task)$ TaskResult
    }

    TaskController --> TaskUseCase
    TaskController ..> CurrentUser : id do usuário do JWT
    TaskController ..> TaskResult : resposta JSON
    TaskUseCase --> TaskRepository : porta de saída
    TaskUseCase ..> Task : carrega, altera, salva
    TaskUseCase ..> TaskResult : converte
    Task --|> AggregateRoot
    Task ..> AgendaTitle : valida título
    Task ..> InvalidTaskTransitionException : lança
    TaskRepositoryAdapter ..|> TaskRepository
    TaskRepositoryAdapter --> TaskJpaRepository
    TaskJpaRepository ..> TaskEntity
    TaskRepositoryAdapter ..> Task : mapeia Entity e Domínio
```

**Pontos para a apresentação:** a seta `TaskUseCase --> TaskRepository` aponta para uma **interface do
domínio**, não para o JPA (inversão de dependência, o "D" do SOLID). É isso que permite testar o
`TaskUseCase` com Mockito sem banco (`TaskUseCaseTest`) e o `Task` com JUnit puro (`TaskTest`).

---

## 2. Autenticação e autorização com JWT

O login passa pelo `AuthenticationUseCase`, que depende apenas de portas (`PasswordHasher`, `TotpService`,
`TokenIssuer`...). A infraestrutura fornece Argon2id, TOTP e a emissão do JWT assinado com RSA. Nas
requisições seguintes, o Spring Security valida o token com o `JwtDecoder` (mesmo par de chaves) e
converte a claim `role` em autoridade (`ROLE_TITULAR`/`ROLE_APOIO`); os controllers leem o usuário via
`CurrentUser`.

```mermaid
classDiagram
    direction TB

    class AuthController {
        <<RestController>>
        +login(LoginRequest) TokenResponse
        +refresh(RefreshRequest) TokenResponse
        +logout(LogoutRequest) void
    }
    class AuthenticationUseCase {
        <<Service>>
        +login(LoginCommand) AuthResult
        +refresh(RefreshTokenCommand) AuthResult
        +logout(LogoutCommand) void
        -verifyTotpIfRequired(User, String) void
        -issueTokens(User, String) AuthResult
    }
    class UserRepository {
        <<interface>>
        +findByEmail(Email) Optional~User~
    }
    class SessionRepository {
        <<interface>>
        +save(RefreshSession) RefreshSession
        +findByTokenHash(String) Optional~RefreshSession~
        +revokeAllByUserId(UserId) void
    }
    class PasswordHasher {
        <<interface>>
        +hash(String) Password
        +matches(String, Password) boolean
    }
    class TotpService {
        <<interface>>
        +verify(TotpSecret, String) boolean
    }
    class TokenIssuer {
        <<interface>>
        +issueAccessToken(User) IssuedAccessToken
    }
    class Argon2PasswordHasher
    class SamstevensTotpService
    class JwtTokenIssuer {
        -JwtEncoder jwtEncoder
    }
    class UserRepositoryAdapter
    class SessionRepositoryAdapter
    class AuthorizationServerConfig {
        <<Configuration>>
        +jwkSource() JWKSource
        +jwtEncoder(JWKSource) JwtEncoder
        +jwtDecoder(JWKSource) JwtDecoder
    }
    class SecurityConfig {
        <<Configuration>>
        +defaultSecurityFilterChain(HttpSecurity) SecurityFilterChain
        +jwtAuthenticationConverter() JwtAuthenticationConverter
    }
    class CurrentUser {
        <<utility>>
        +require()$ AuthenticatedUser
    }

    AuthController --> AuthenticationUseCase
    AuthenticationUseCase --> UserRepository
    AuthenticationUseCase --> SessionRepository : refresh token opaco
    AuthenticationUseCase --> PasswordHasher
    AuthenticationUseCase --> TotpService : 2FA
    AuthenticationUseCase --> TokenIssuer : access token 15 min
    UserRepositoryAdapter ..|> UserRepository
    SessionRepositoryAdapter ..|> SessionRepository
    Argon2PasswordHasher ..|> PasswordHasher
    SamstevensTotpService ..|> TotpService
    JwtTokenIssuer ..|> TokenIssuer
    AuthorizationServerConfig ..> JwtTokenIssuer : fornece JwtEncoder
    AuthorizationServerConfig ..> SecurityConfig : fornece JwtDecoder
    SecurityConfig ..> CurrentUser : SecurityContext com Jwt
```

---

## 3. Integração entre módulos sem acoplamento direto

Os módulos não chamam classes internas uns dos outros (ADR-001). Há dois mecanismos:

**3a. Evento de domínio** — ao revogar um vínculo, o agregado `SupportLink` registra `LinkRevoked`; o caso
de uso salva e publica; o módulo **Identity** reage derrubando as sessões do Apoio (RF-01.4). O Link não
sabe que o Identity existe.

**3b. Porta compartilhada** — o módulo **Emotional** precisa saber se um Apoio pode ver os dados do
Titular, mas não pode importar o Link. Ele depende da interface `SupportAccessPolicy`, publicada no
`shared`; a implementação vive no Link e é injetada pelo Spring.

```mermaid
classDiagram
    direction LR

    class LinkLifecycleUseCase {
        <<Service · link>>
        +revoke(RevokeLinkCommand) void
    }
    class SupportLink {
        <<aggregate · link>>
        +revoke() void
        +hasPermission(ModulePermission) boolean
        +isActive() boolean
    }
    class AggregateRoot {
        <<abstract · shared>>
        #registerEvent(DomainEvent) void
        +pullDomainEvents() List~DomainEvent~
    }
    class DomainEvent {
        <<interface · shared>>
    }
    class LinkRevoked {
        <<record · link>>
        +linkId() String
        +titularId() String
        +apoioId() String
    }
    class DomainEventPublisher {
        <<Component · shared>>
        +publishFrom(AggregateRoot) void
    }
    class ApplicationEventPublisher {
        <<interface · Spring>>
    }
    class LinkRevokedListener {
        <<Component · identity>>
        +onLinkRevoked(LinkRevoked) void
    }
    class SessionRepository {
        <<interface · identity>>
        +revokeAllByUserId(UserId) void
    }
    class SupportLinkRepository {
        <<interface · link>>
        +findActiveBetween(UserId, UserId) Optional~SupportLink~
    }

    class EmotionalUseCase {
        <<Service · emotional>>
        +getTrend(String requesterId, String ownerId, int days) MoodTrendResult
    }
    class SupportAccessPolicy {
        <<interface · shared>>
        +hasModuleAccess(UserId, UserId, String) boolean
        +requireModuleAccess(UserId, UserId, String) void
    }
    class SupportAccessPolicyImpl {
        <<Component · link>>
    }

    LinkLifecycleUseCase --> SupportLinkRepository
    LinkLifecycleUseCase --> DomainEventPublisher
    LinkLifecycleUseCase ..> SupportLink : revoke e save
    SupportLink --|> AggregateRoot
    SupportLink ..> LinkRevoked : registerEvent
    LinkRevoked ..|> DomainEvent
    DomainEventPublisher --> ApplicationEventPublisher
    ApplicationEventPublisher ..> LinkRevokedListener : entrega o evento
    LinkRevokedListener --> SessionRepository : derruba sessões do Apoio

    EmotionalUseCase --> SupportAccessPolicy : depende só do contrato
    SupportAccessPolicyImpl ..|> SupportAccessPolicy
    SupportAccessPolicyImpl --> SupportLinkRepository
```

---

## 4. Isolamento do Cofre (zero-knowledge)

O Cofre tem **cadeia de segurança própria**, que exige `ROLE_TITULAR` para qualquer rota `/api/v1/vault/**`.
Um APOIO é barrado **antes** de chegar ao controller — não existe checagem configurável que possa ser
esquecida (RF-08.5). A tentativa negada é auditada. A persistência usa **datasource, EntityManager,
TransactionManager e Flyway próprios** (schema `ng_vault`), configurados no próprio módulo.

```mermaid
classDiagram
    direction TB

    class SecurityConfig {
        <<Configuration · shared>>
        +vaultSecurityFilterChain(HttpSecurity, AccessDeniedHandler) SecurityFilterChain
    }
    class VaultAccessDeniedHandler {
        <<AccessDeniedHandler>>
        +handle(request, response, exception) void
    }
    class VaultController {
        <<RestController>>
    }
    class VaultUseCase {
        <<Service>>
        +setup(SetupVaultCommand) VaultSetupResult
        +open(OpenVaultCommand) VaultSessionResult
        +createEntry(String sessionToken, CreateVaultEntryCommand) VaultEntryResult
        +listEntries(String sessionToken) List~VaultEntryResult~
        +lock(String sessionToken) void
    }
    class VaultRepository {
        <<interface>>
        +save(VaultEntry) VaultEntry
        +findByOwnerId(UserId) List~VaultEntry~
    }
    class MasterKeyMetaRepository {
        <<interface>>
        +save(MasterKeyMeta) MasterKeyMeta
    }
    class VaultSessionService {
        <<interface>>
        +open(UserId) OpenedSession
        +validate(String) UserId
        +invalidate(String) void
    }
    class VaultAuditLogger {
        <<interface>>
        +log(UserId, String action, ...) void
    }
    class VaultRepositoryAdapter
    class MasterKeyMetaRepositoryAdapter
    class InMemoryVaultSessionService {
        auto-lock 5 min
    }
    class VaultAuditLoggerAdapter {
        ng_vault_auth_logs
    }
    class VaultDataSourceConfig {
        <<Configuration>>
        +vaultDataSource() DataSource
        +vaultEntityManagerFactory() LocalContainerEntityManagerFactoryBean
        +vaultTransactionManager() PlatformTransactionManager
    }

    SecurityConfig ..> VaultAccessDeniedHandler : ROLE_APOIO negado
    SecurityConfig ..> VaultController : só ROLE_TITULAR chega
    VaultAccessDeniedHandler --> VaultAuditLogger : audita tentativa
    VaultController --> VaultUseCase
    VaultUseCase --> VaultRepository
    VaultUseCase --> MasterKeyMetaRepository
    VaultUseCase --> VaultSessionService
    VaultUseCase --> VaultAuditLogger
    VaultRepositoryAdapter ..|> VaultRepository
    MasterKeyMetaRepositoryAdapter ..|> MasterKeyMetaRepository
    InMemoryVaultSessionService ..|> VaultSessionService
    VaultAuditLoggerAdapter ..|> VaultAuditLogger
    VaultDataSourceConfig ..> VaultRepositoryAdapter : persistência isolada
```

O teste de integração `NeuroGenIntegrationTest` (executado no CI com PostgreSQL real) comprova essa
interação: um token APOIO recebe **403** em `/api/v1/vault/entries` e a tentativa aparece em
`ng_vault_auth_logs`.

---

## 5. Tratamento de erros padronizado

Qualquer camada lança exceções da hierarquia `NeuroGenException`, que já carregam código e status HTTP. O
`GlobalExceptionHandler` (um `@RestControllerAdvice`) é o **único** ponto que transforma exceções em
resposta, sempre no formato `ErrorResponse` (inspirado na RFC 7807). No Checkpoint 3 foram adicionados,
via TDD, os handlers para erros de requisição tratados pelo Spring MVC, que antes viravam HTTP 500.

```mermaid
classDiagram
    direction LR

    class NeuroGenException {
        <<abstract>>
        +getErrorCode() String
        +getHttpStatus() HttpStatus
    }
    class DomainException {
        <<abstract>>
    }
    class ValidationException {
        400
        +getFieldErrors() Map
    }
    class NotFoundException {
        404
    }
    class ForbiddenAccessException {
        403
    }
    class InvalidTaskTransitionException {
        409
    }
    class EventAlreadyCancelledException {
        409
    }
    class GlobalExceptionHandler {
        <<RestControllerAdvice>>
        +handleNeuroGen(NeuroGenException, request) ResponseEntity
        +handleBeanValidation(MethodArgumentNotValidException, request) ResponseEntity
        +handleUnreadableBody(HttpMessageNotReadableException, request) ResponseEntity
        +handleMissingParameter(MissingServletRequestParameterException, request) ResponseEntity
        +handleTypeMismatch(MethodArgumentTypeMismatchException, request) ResponseEntity
        +handleMethodNotSupported(HttpRequestMethodNotSupportedException, request) ResponseEntity
        +handleMediaType(HttpMediaTypeNotSupportedException, request) ResponseEntity
        +handleNoRoute(Exception, request) ResponseEntity
        +handleUnexpected(Exception, request) ResponseEntity
    }
    class ErrorResponse {
        <<record>>
        timestamp, status, error
        code, message, path
        fieldErrors
    }

    DomainException --|> NeuroGenException
    ValidationException --|> DomainException
    NotFoundException --|> DomainException
    ForbiddenAccessException --|> DomainException
    InvalidTaskTransitionException --|> DomainException
    EventAlreadyCancelledException --|> DomainException
    GlobalExceptionHandler ..> NeuroGenException : traduz
    GlobalExceptionHandler ..> ErrorResponse : produz
```

| Situação | Antes | Depois (Checkpoint 3) |
|----------|-------|------------------------|
| JSON malformado | 500 | 400 `MALFORMED_REQUEST` |
| Parâmetro obrigatório ausente | 500 | 400 `MISSING_PARAMETER` |
| Parâmetro com formato inválido | 500 | 400 `INVALID_PARAMETER` |
| Método HTTP não suportado | 500 | 405 `METHOD_NOT_ALLOWED` + cabeçalho `Allow` |
| Content-Type não suportado | 500 | 415 `UNSUPPORTED_MEDIA_TYPE` |
| Rota inexistente | 500 | 404 `RESOURCE_NOT_FOUND` |
| Filtro `?status=` inválido em tarefas | 500 | 400 `VALIDATION_ERROR` |
| Concluir tarefa arquivada / alterar evento cancelado | aceito | 409 |
