# Diagramas de Sequência — Neuro-Gen

> **Fluxos documentados:**
> 1. Registro de conta + verificação de e-mail
> 2. Login com credenciais (JWT + 2FA opcional)
> 3. Criação e aceite de vínculo Titular/Apoio
> 4. Revogação de vínculo com invalidação de sessão em tempo real
> 5. Criar tarefa com lembrete persistente (nudge)
> 6. Fluxo de sessão de foco (start → pause → resume → complete)
> 7. Acesso ao Cofre — zero-knowledge (open → create entry → auto-lock)
> 8. Check-in emocional com acesso por Apoio autorizado
> 9. Tentativa de acesso ao Vault por conta Apoio (fluxo de segurança)
>
> **Participantes recorrentes:**
> - `Client` — Web App (React) ou Mobile App (KMM)
> - `BFF` — BFF / API Gateway (Spring Boot)
> - `CoreService` — Monolito modular
> - `VaultService` — Serviço isolado (mTLS)
> - `NotifService` — Notification Service (consumer RabbitMQ)
> - `CoreDB` — PostgreSQL Core
> - `VaultDB` — PostgreSQL Vault (banco separado)
> - `Queue` — RabbitMQ

---

## SEQ-01: Registro de Conta + Verificação de E-mail

> Fluxo: cadastro → e-mail de verificação → ativação da conta (RF-01.1, RF-01.6).
> Conta permanece em `PENDING_VERIFICATION` até verificação — funcionalidades limitadas.

```mermaid
sequenceDiagram
    actor Titular
    participant Client
    participant BFF
    participant CoreService
    participant CoreDB
    participant EmailProvider

    Titular->>Client: Preenche formulário de cadastro
    Client->>BFF: POST /api/v1/auth/register\n{email, password, role}

    Note over BFF: Valida RegisterRequest\n(Bean Validation)

    BFF->>CoreService: RegisterUserCommand\n(email, rawPassword, role)

    Note over CoreService: User.register(cmd)\n→ Password.hash(rawPassword)\n→ Argon2id (m=19MB,t=2,p=1)\n→ Cria User com status=PENDING_VERIFICATION

    CoreService->>CoreDB: INSERT ng_identity.ng_users\n(id, email, password_hash, role, status)

    CoreService->>CoreDB: INSERT ng_identity.ng_email_verifications\n(user_id, token_hash, expires_at=+24h)

    Note over CoreService: Publica UserRegistered\n(DomainEvent)

    CoreService->>EmailProvider: Envia e-mail de verificação\n(token + link de ativação)

    CoreService-->>BFF: UserResult\n{id, email, role, status=PENDING_VERIFICATION}
    BFF-->>Client: HTTP 201 Created\n{user: UserProfileResponse}

    Client-->>Titular: Exibe tela "Verifique seu e-mail"

    Note over Titular: Clica no link no e-mail

    Titular->>Client: GET /verify?token=abc123\n(deeplink ou web)
    Client->>BFF: POST /api/v1/auth/verify-email\n{userId, token}
    BFF->>CoreService: VerifyEmailCommand\n{userId, token}

    Note over CoreService: Busca token_hash no banco\nValida não expirado e não usado\nChama user.verifyEmail()\nAtualiza status=ACTIVE

    CoreService->>CoreDB: UPDATE ng_users SET email_verified=true\nUPDATE ng_email_verifications SET used_at=now()

    CoreService-->>BFF: UserResult\n{status=ACTIVE, emailVerified=true}
    BFF-->>Client: HTTP 200 OK
    Client-->>Titular: Redireciona para login\n("E-mail verificado! Faça login.")
```

---

## SEQ-02: Login com Credenciais (JWT + 2FA Opcional)

> Fluxo normal e fluxo com TOTP (RF-01.9).
> Access token: JWT 15min (stateless). Refresh token: 30 dias (persistido).
> Rate limit: máx. 5 tentativas/min por IP (RNF-SEC.6).

```mermaid
sequenceDiagram
    actor Titular
    participant Client
    participant BFF
    participant CoreService
    participant CoreDB

    Titular->>Client: Informa email e senha
    Client->>BFF: POST /api/v1/auth/login\n{email, password}

    Note over BFF: Rate limiting por IP\n(máx. 5 req/min — RNF-SEC.6)

    BFF->>CoreService: LoginCommand\n{email, rawPassword, totpCode=null}
    CoreService->>CoreDB: SELECT ng_users WHERE email=?

    alt Email não encontrado ou senha incorreta
        CoreService-->>BFF: throws InvalidCredentialsException
        BFF-->>Client: HTTP 401\n{code: AUTH_INVALID_CREDENTIALS\n"Email ou senha incorretos"}
        Note over Client: Mensagem genérica —\nnão revela qual campo está errado
    else Conta PENDING_VERIFICATION
        CoreService-->>BFF: throws EmailNotVerifiedException
        BFF-->>Client: HTTP 403\n{code: EMAIL_NOT_VERIFIED}
    else Credenciais válidas + 2FA ATIVO
        Note over CoreService: user.requiresTotp() == true
        CoreService-->>BFF: throws TotpRequiredException
        BFF-->>Client: HTTP 401\n{code: TOTP_REQUIRED\nstep: "MFA"}

        Note over Titular: Informa código TOTP do app autenticador
        Client->>BFF: POST /api/v1/auth/login\n{email, password, totpCode: "123456"}
        BFF->>CoreService: LoginCommand\n{email, rawPassword, totpCode}
        CoreService->>CoreDB: SELECT ng_totp_secrets WHERE user_id=?
        Note over CoreService: totpSecret.verifyCode(totpCode)\n(janela TOTP de 30s)

        alt TOTP inválido
            CoreService-->>BFF: throws InvalidTotpCodeException
            BFF-->>Client: HTTP 401\n{code: TOTP_INVALID}
        else TOTP válido — continua abaixo
        end
    else Credenciais válidas + sem 2FA
    end

    Note over CoreService: Gera accessToken (JWT 15min)\nGera refreshToken (opaco)\nPersiste hash do refreshToken

    CoreService->>CoreDB: INSERT ng_sessions\n(user_id, refresh_token_hash, expires_at=+30d)

    CoreService-->>BFF: AuthResult\n{accessToken, refreshToken, expiresIn, user}
    BFF-->>Client: HTTP 200 OK\n{TokenResponse + UserProfileResponse}
    Client-->>Titular: Redireciona para dashboard
```

---

## SEQ-03: Criação e Aceite de Vínculo Titular/Apoio

> Titular gera convite → Apoio aceita via token (TTL 72h — RF-01.2).
> Sem permissões concedidas no aceite — Titular configura depois (opt-in RF-01.3).

```mermaid
sequenceDiagram
    actor Titular
    actor Apoio
    participant Client as Client (Titular)
    participant ClientApoio as Client (Apoio)
    participant BFF
    participant CoreService
    participant CoreDB
    participant EmailProvider

    Note over Titular: Quer vincular um Apoio
    Titular->>Client: Clica em "Convidar Apoio"
    Client->>BFF: POST /api/v1/links/invite\nAuthorization: Bearer {accessToken}

    Note over BFF: Valida JWT\nExtrai userId do claims

    BFF->>CoreService: CreateInviteCommand\n{titularId}
    Note over CoreService: SupportLink.createInvite(titularId)\n→ InviteToken.generate() (72h TTL)\n→ status=PENDING\n→ PermissionMatrix.empty()

    CoreService->>CoreDB: INSERT ng_core.ng_support_links\n(id, titular_id, invite_token_hash, status=PENDING)

    CoreService-->>BFF: LinkResult\n{linkId, inviteToken, expiresAt}
    BFF-->>Client: HTTP 201\n{InviteResponse: linkId, inviteUrl, expiresAt}
    Client-->>Titular: Exibe link/QR code para compartilhar\ncom o Apoio

    Note over Titular: Compartilha o convite com o Apoio

    Apoio->>ClientApoio: Clica no link de convite
    ClientApoio->>BFF: POST /api/v1/links/accept\n{inviteToken}\nAuthorization: Bearer {accessToken Apoio}

    BFF->>CoreService: AcceptInviteCommand\n{rawInviteToken, apoioId}

    CoreService->>CoreDB: SELECT ng_support_links\nWHERE invite_token_hash=SHA256(token)

    alt Token não encontrado
        CoreService-->>BFF: throws NotFoundException
        BFF-->>ClientApoio: HTTP 404
    else Token expirado (> 72h)
        CoreService-->>BFF: throws InviteExpiredException
        BFF-->>ClientApoio: HTTP 410 Gone\n{code: INVITE_EXPIRED}
    else Token já usado
        CoreService-->>BFF: throws InviteAlreadyUsedException
        BFF-->>ClientApoio: HTTP 409 Conflict
    else Token válido
        Note over CoreService: link.accept(apoioId)\n→ status=ACTIVE\n→ permissions mantidas vazias (opt-in)

        CoreService->>CoreDB: UPDATE ng_support_links\nSET apoio_id=?, status=ACTIVE, accepted_at=NOW()

        Note over CoreService: Publica LinkCreated (DomainEvent)
        CoreService-->>BFF: LinkResult\n{status=ACTIVE, grantedPermissions=[]}
        BFF-->>ClientApoio: HTTP 200 OK\n{"Vínculo criado! Aguarde o Titular liberar módulos."}
    end

    Note over Titular: Recebe notificação de vínculo aceito
    Titular->>Client: Configura permissões do Apoio
    Client->>BFF: PATCH /api/v1/links/{linkId}/permissions\n{grant: ["AGENDA", "FOCUS_HISTORY"]}

    BFF->>CoreService: UpdatePermissionsCommand
    CoreService->>CoreDB: INSERT ng_link_permissions (link_id, module_name, granted_by, granted_at)
    BFF-->>Client: HTTP 200 OK\n{LinkResponse com permissions atualizadas}
```

---

## SEQ-04: Revogação de Vínculo com Invalidação de Sessão

> Revogação tem efeito em < 5s — sessão ativa do Apoio perde acesso imediatamente (RF-01.4).

```mermaid
sequenceDiagram
    actor Titular
    participant Client
    participant BFF
    participant CoreService
    participant CoreDB
    participant RedisCache as Redis (Session Cache)

    Titular->>Client: Clica em "Revogar vínculo" do Apoio X
    Client->>BFF: Exibe confirmação explícita\n(RNF-USA.7)

    Titular->>Client: Confirma revogação
    Client->>BFF: DELETE /api/v1/links/{linkId}\nAuthorization: Bearer {accessToken}

    BFF->>CoreService: RevokeLinkCommand\n{linkId, titularId}

    CoreService->>CoreDB: SELECT ng_support_links WHERE id=?\nAND titular_id=?

    alt Vínculo não encontrado ou não pertence ao Titular
        CoreService-->>BFF: throws LinkNotFoundException / ForbiddenAccessException
        BFF-->>Client: HTTP 404 / 403
    else Vínculo já revogado
        CoreService-->>BFF: throws LinkAlreadyRevokedException
        BFF-->>Client: HTTP 409
    else Vínculo ativo
        Note over CoreService: link.revoke()\n→ status=REVOKED, revoked_at=NOW()\nPublica LinkRevoked (DomainEvent)

        CoreService->>CoreDB: UPDATE ng_support_links SET status=REVOKED, revoked_at=NOW()
        CoreService->>CoreDB: DELETE FROM ng_link_permissions WHERE link_id=?

        Note over CoreService: Listener de LinkRevoked:\nInvalida todas as sessões ativas do Apoio
        CoreService->>CoreDB: UPDATE ng_sessions SET revoked_at=NOW()\nWHERE user_id=apoioId AND revoked_at IS NULL

        CoreService->>RedisCache: INVALIDATE sessions:apoioId\n(propagação em < 5s — RF-01.4)

        CoreService-->>BFF: Sucesso
        BFF-->>Client: HTTP 204 No Content
        Client-->>Titular: "Vínculo encerrado. O Apoio perdeu acesso imediatamente."
    end

    Note over Apoio: Na próxima requisição do Apoio ao BFF:\nJWT válido mas sessão inválida no Redis\nRetorna HTTP 401 — Apoio é redirecionado para login
```

---

## SEQ-05: Criar Tarefa com Lembrete Persistente (Nudge)

> Nudge: lembrete que se repete M vezes a cada N minutos até confirmação (RF-03.2).
> Dispatcher no Notification Service consome fila RabbitMQ.

```mermaid
sequenceDiagram
    actor Titular
    participant Client
    participant BFF
    participant CoreService
    participant CoreDB
    participant Queue as RabbitMQ
    participant NotifService
    participant PushProvider as FCM/APNs

    Titular->>Client: Cria tarefa com lembrete persistente\n(5min antes, repetir a cada 3min, máx 3x)

    Client->>BFF: POST /api/v1/tasks\n{title, dueDate, reminders: [{minutesBefore:5, persistent:true, repeatInterval:3, maxRep:3}]}

    BFF->>CoreService: CreateTaskCommand

    Note over CoreService: Task.create(cmd)\n→ Reminder criado: status=SCHEDULED\n→ nextFireAt = dueDate - 5min

    CoreService->>CoreDB: INSERT ng_tasks + INSERT ng_reminders\n(status=SCHEDULED, next_fire_at=?)
    CoreService-->>BFF: TaskResult
    BFF-->>Client: HTTP 201 Created {TaskResponse}
    Client-->>Titular: Tarefa criada com lembrete

    Note over CoreService: Scheduler (job periódico — a cada 30s):\nSELECT ng_reminders WHERE status=SCHEDULED\nAND next_fire_at <= NOW()

    CoreService->>CoreDB: SELECT reminders a disparar
    CoreDB-->>CoreService: [Reminder do Titular]

    CoreService->>Queue: Publica ReminderFireEvent\n{userId, reminderId, channel=PUSH}
    CoreService->>CoreDB: UPDATE ng_reminders SET status=SENT

    NotifService->>Queue: Consome ReminderFireEvent
    NotifService->>CoreDB: SELECT ng_notification_channels\nWHERE user_id=? AND channel_type=PUSH AND active=true

    NotifService->>PushProvider: Envia push notification
    NotifService->>CoreDB: INSERT ng_notification_logs\n(status=SENT, sent_at=NOW())

    alt Titular confirma o lembrete
        Titular->>Client: Clica na notificação / confirma leitura
        Client->>BFF: POST /api/v1/reminders/{id}/confirm
        BFF->>CoreService: ConfirmReminderCommand
        CoreService->>CoreDB: UPDATE ng_reminders SET status=CONFIRMED, confirmation_at=NOW()
        Note over CoreService: Nudge encerrado — confirmação recebida
    else Titular não confirma em 3min
        Note over CoreService: Scheduler detecta: status=SENT\nnext_fire_at expirado\npersistent=true, currentRep(1) < maxRep(3)
        CoreService->>CoreDB: UPDATE next_fire_at += repeatIntervalMinutes\nSET current_repetitions=2
        CoreService->>Queue: Publica novo ReminderFireEvent (2ª tentativa)
        Note over NotifService: Envia 2ª notificação push

        Note over CoreService: Após 3ª tentativa sem confirmação:
        CoreService->>CoreDB: UPDATE status=UNCONFIRMED_LIMIT_REACHED
        Note over Client: Dashboard marca tarefa como\n"lembrete não confirmado" (RF-03.2)
    end
```

---

## SEQ-06: Sessão de Foco (Start → Pause → Resume → Complete)

> Ao iniciar, suprime notificações não críticas (RF-04.2).
> Retomar sessão restaura tempo acumulado sem reiniciar (RF-04.8).

```mermaid
sequenceDiagram
    actor Titular
    participant Client
    participant BFF
    participant CoreService
    participant CoreDB
    participant Queue as RabbitMQ
    participant NotifService

    Titular->>Client: Clica "Iniciar Foco"\n(25min, tarefa vinculada)

    Client->>BFF: POST /api/v1/focus/sessions\n{taskId, durationMinutes:25, breakMinutes:5}
    BFF->>CoreService: StartFocusSessionCommand

    CoreService->>CoreDB: SELECT ng_focus_sessions\nWHERE user_id=? AND status=RUNNING
    alt Sessão já ativa
        CoreService-->>BFF: throws SessionAlreadyActiveException
        BFF-->>Client: HTTP 409\n"Você já tem uma sessão de foco ativa"
    else Nenhuma sessão ativa
        Note over CoreService: FocusSession.start(cmd)\nPublica FocusSessionStarted (DomainEvent)
        CoreService->>CoreDB: INSERT ng_focus_sessions\n(status=RUNNING, started_at=NOW())
        CoreService->>Queue: Publica FocusSessionStarted\n{userId, sessionId}
    end

    NotifService->>Queue: Consome FocusSessionStarted
    Note over NotifService: Marca userId como "em foco"\nNotificações não críticas ficam em fila

    BFF-->>Client: HTTP 201\n{FocusSessionResponse, status=RUNNING}
    Client-->>Titular: Exibe timer regressivo

    Note over Titular: Após 10 minutos, precisa pausar
    Titular->>Client: Clica "Pausar"
    Client->>BFF: POST /api/v1/focus/sessions/{id}/pause
    BFF->>CoreService: PauseFocusSessionCommand
    Note over CoreService: session.pause()\n→ cria PauseRecord(pausedAt=NOW())\n→ status=PAUSED
    CoreService->>CoreDB: INSERT ng_focus_pauses (paused_at=NOW())\nUPDATE ng_focus_sessions SET status=PAUSED
    BFF-->>Client: HTTP 200\n{status=PAUSED}

    Note over Titular: Retoma após 3 minutos
    Titular->>Client: Clica "Retomar"
    Client->>BFF: POST /api/v1/focus/sessions/{id}/resume
    BFF->>CoreService: ResumeFocusSessionCommand
    Note over CoreService: session.resume()\n→ PauseRecord.end(NOW())\n→ status=RUNNING\n→ accumulatedSeconds += pauseDuration
    CoreService->>CoreDB: UPDATE ng_focus_pauses SET resumed_at=NOW()\nUPDATE ng_focus_sessions SET status=RUNNING
    BFF-->>Client: HTTP 200\n{status=RUNNING, effectiveSeconds=600}

    Note over Titular: Completa a sessão
    Titular->>Client: Clica "Finalizar"
    Client->>BFF: POST /api/v1/focus/sessions/{id}/complete
    BFF->>CoreService: CompleteFocusSessionCommand
    Note over CoreService: session.complete()\nPublica FocusSessionEnded
    CoreService->>CoreDB: UPDATE ng_focus_sessions\nSET status=COMPLETED, ended_at=NOW()
    CoreService->>Queue: Publica FocusSessionEnded\n{userId, sessionId, finalStatus=COMPLETED}

    NotifService->>Queue: Consome FocusSessionEnded
    Note over NotifService: Remove userId do estado "em foco"\nEntrega notificações retidas na fila

    BFF-->>Client: HTTP 200\n{status=COMPLETED, effectiveSeconds=1420}
    Client-->>Titular: Exibe resumo da sessão\n(tempo efetivo, tarefa vinculada)
```

---

## SEQ-07: Cofre — Abrir, Criar Entrada, Auto-lock (Zero-Knowledge)

> Chave derivada no CLIENTE com Argon2id + AES-256-GCM.
> Servidor armazena e devolve bytes cifrados — nunca interpreta (ADR-005, RNF-SEC.3).
> mTLS obrigatório entre BFF e VaultService.

```mermaid
sequenceDiagram
    actor Titular
    participant Client
    participant BFF
    participant VaultService
    participant VaultDB as VaultDB (isolado)

    Note over Titular: Clica em "Cofre"
    Titular->>Client: Digita senha mestra
    Note over Client: Derivação de chave NO CLIENTE:\n1. Busca argon2_params do servidor\n2. Argon2id(masterPassword, salt, params) → chaveAES\n3. Hash(masterPassword) → para autenticação no servidor\n(Chave AES nunca sai do dispositivo)

    Client->>VaultService: GET /vault/key-params\n(via BFF com mTLS)
    VaultService->>VaultDB: SELECT ng_master_key_meta WHERE user_id=?
    VaultDB-->>VaultService: {argon2_params, configured_at}
    VaultService-->>Client: {argon2Params}

    Note over Client: Deriva chave localmente (Argon2id)\nGera masterPasswordHash para autenticar

    Client->>BFF: POST /api/v1/vault/open\n{masterPasswordHash}\nAuthorization: Bearer {JWT}
    BFF->>VaultService: POST /internal/vault/open\n(mTLS)\n{userId, masterPasswordHash}

    VaultService->>VaultDB: SELECT ng_master_key_meta WHERE user_id=?
    Note over VaultService: Verifica masterPasswordHash\n(hash do hash da senha)\nServer-side — sem ver a senha

    alt masterPasswordHash inválido
        VaultService-->>BFF: HTTP 401 InvalidMasterKeyException
        VaultService->>VaultDB: INSERT ng_vault_auth_logs (success=false)
        BFF-->>Client: HTTP 401\n{code: VAULT_INVALID_MASTER_KEY}
    else Válido
        VaultService->>VaultDB: INSERT ng_vault_auth_logs (action=OPEN, success=true)
        Note over VaultService: Gera vaultSessionToken (5min TTL)
        VaultService-->>BFF: {vaultSessionToken, expiresIn=300}
        BFF-->>Client: {VaultSessionResponse}
        Client-->>Titular: Cofre desbloqueado — exibe lista de entradas
    end

    Note over Titular: Cria nova entrada (login/senha de um site)
    Note over Client: Cifra no CLIENTE:\nciphertext = AES-256-GCM(content, chaveAES, iv)\npayload = {ciphertext, iv, salt} → Base64JSON

    Titular->>Client: Preenche label + credenciais
    Client->>BFF: POST /api/v1/vault/entries\n{label, entryType=LOGIN, encryptedPayload=Base64JSON}\nvaultSessionToken: {token}

    BFF->>VaultService: POST /internal/vault/entries (mTLS)\n{userId, label, entryType, encryptedPayload}

    Note over VaultService: Armazena ciphertext opaquely\nNão interpreta o conteúdo

    VaultService->>VaultDB: INSERT ng_vault_entries\n(owner_id, label, entry_type, ciphertext, iv, salt)
    VaultService->>VaultDB: INSERT ng_vault_auth_logs (action=ENTRY_CREATE, success=true)
    VaultService-->>BFF: {VaultEntryResult: id, label, entryType}
    BFF-->>Client: HTTP 201\n{VaultEntryResponse}

    Note over Client: Titular inativo por 5 minutos (RF-08.6)
    Note over Client: Auto-lock local: limpa chaveAES da memória\n(a chave AES nunca foi ao servidor)

    Client->>BFF: POST /api/v1/vault/lock\n{vaultSessionToken}
    BFF->>VaultService: POST /internal/vault/lock
    VaultService->>VaultDB: INSERT ng_vault_auth_logs (action=LOCK)
    Note over VaultService: Invalida vaultSessionToken
    BFF-->>Client: HTTP 200

    Client-->>Titular: Cofre bloqueado — senha mestra necessária para reabrir
```

---

## SEQ-08: Check-in Emocional + Acesso por Apoio Autorizado

> Dado sensível de saúde. Apoio acessa somente com permissão EMOTIONAL_CHECKIN ativa.
> Disclaimer obrigatório em toda exibição de insight emocional (RF-05).

```mermaid
sequenceDiagram
    actor Titular
    actor Apoio
    participant ClientT as Client (Titular)
    participant ClientA as Client (Apoio)
    participant BFF
    participant CoreService
    participant CoreDB as CoreDB (ng_sensitive)

    Titular->>ClientT: Faz check-in\n(humor=4, energia=3, notas="Dia produtivo")
    ClientT->>BFF: POST /api/v1/emotional/checkins\n{moodScore:4, energyLevel:3, notes}\nAuthorization: Bearer {JWT Titular}

    BFF->>CoreService: SubmitMoodCheckInCommand\n{userId, moodScore, energyLevel, notes}

    Note over CoreService: MoodCheckIn.record(cmd)\n→ MoodScore(4).toLevel() = HIGH

    CoreService->>CoreDB: INSERT ng_sensitive.ng_mood_checkins\n(user_id, mood_score, energy_level,\nnotes_enc=pgcrypto_encrypt(notes))
    %% notes cifrado com pgcrypto antes de persistir

    CoreService-->>BFF: MoodCheckInResult\n{moodLevel=HIGH}
    BFF-->>ClientT: HTTP 201\n{MoodCheckInResponse}
    ClientT-->>Titular: "Check-in registrado! 😊"

    Note over Apoio: Quer ver o estado emocional do Titular
    Apoio->>ClientA: Acessa módulo "Humor do Titular"
    ClientA->>BFF: GET /api/v1/emotional/checkins?titularId={id}\nAuthorization: Bearer {JWT Apoio}

    BFF->>CoreService: GetEmotionalDataForApoioQuery\n{apoioId, titularId}

    Note over CoreService: Verifica permissão:\n1. Busca SupportLink ACTIVE entre apoioId e titularId\n2. Verifica ng_link_permissions.module_name=EMOTIONAL_CHECKIN\n3. Ausência = acesso negado (opt-in — RF-01.3)

    CoreService->>CoreDB: SELECT ng_link_permissions\nWHERE link_id=? AND module_name=EMOTIONAL_CHECKIN

    alt Permissão não concedida
        CoreService-->>BFF: throws InsufficientPermissionException\n(module=EMOTIONAL_CHECKIN)
        BFF-->>ClientA: HTTP 403\n{code: INSUFFICIENT_PERMISSION\nmessage: "O Titular não concedeu acesso a este módulo"}
    else Permissão concedida
        CoreService->>CoreDB: SELECT ng_mood_checkins\nWHERE user_id=titularId ORDER BY checkin_date DESC

        Note over CoreService: Descriptografa notes apenas\nse Apoio tiver permissão JOURNAL também\n(granularidade de campo — princípio de minimização)

        CoreService-->>BFF: MoodTrendResult\n{dataPoints, averageMood=4.1, trend=STABLE\ndisclaimer="Estes dados são padrões observados..."}
        BFF-->>ClientA: HTTP 200\n{MoodTrendResponse com disclaimer obrigatório}
        ClientA-->>Apoio: Exibe gráfico de tendência\n+ disclaimer "Não é diagnóstico clínico"
    end
```

---

## SEQ-09: Bloqueio de Acesso ao Vault por Conta Apoio

> Fluxo de segurança: demonstra que a restrição é estrutural (não apenas UI).
> Qualquer tentativa é registrada em log de segurança (RNF-SEC.8, RF-12.4).

```mermaid
sequenceDiagram
    actor Apoio
    participant ClientA as Client (Apoio)
    participant BFF
    participant VaultService
    participant VaultDB
    participant CoreDB as CoreDB (ng_audit_logs)

    Note over Apoio: Apoio tenta acessar o Cofre do Titular\n(diretamente pela URL ou via exploit)

    Apoio->>ClientA: GET /app/vault ou acesso direto à API
    ClientA->>BFF: GET /api/v1/vault/entries\nAuthorization: Bearer {JWT Apoio}

    Note over BFF: Extrai claims do JWT\nRole = APOIO\nVerifica rota autorizada

    Note over BFF: Rota /vault/** NUNCA disponível para role=APOIO\nRestrição hard-coded no SecurityFilterChain\n(não é checagem condicional — RF-08.5, ADR-005)

    BFF-->>ClientA: HTTP 403 Forbidden\n{code: VAULT_ACCESS_DENIED_FOR_APOIO\nmessage: "Módulo Cofre não disponível para esta conta"}

    Note over BFF: Registra tentativa no log de auditoria\n(independente de chegar ao VaultService)

    BFF->>CoreDB: INSERT ng_audit_logs\n(user_id=apoioId, action=VAULT_ACCESS_DENIED_APOIO\nresource_type=VaultService, ip_address)

    Note over VaultService: VaultService sequer recebe a requisição\nRestrição aplicada na camada BFF/Gateway\n(defesa em profundidade — ADR-005)

    Note over CoreDB: Log imutável — sem UPDATE/DELETE\nRetido 12 meses mínimo (RNF-SEC.8)\nAuditável pela equipe de segurança (RF-12.4)

    ClientA-->>Apoio: Tela de erro\n(Cofre não listado no menu do Apoio — UI também ocultada)

    Note over BFF: Se múltiplas tentativas em curto período:\nAcionamento de alerta de segurança (RNF-OBS.5)\n→ Notificação ao time de segurança
```
