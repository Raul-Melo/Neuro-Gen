# API — Identity (Autenticação e Perfil)

Base URL: `http://localhost:8080/api/v1`

> **Primeiro uso:** o banco inicia vazio. Chame `POST /auth/register` antes de tentar login.

---

## POST /auth/register

Cria uma nova conta. O e-mail fica em estado `PENDING_VERIFICATION` até a verificação.

**Body**
```json
{
  "email": "titular@exemplo.com",
  "password": "MinhaS3nha!",
  "role": "TITULAR"
}
```

- `role` aceita `TITULAR` ou `APOIO`
- `password` deve ter entre 8 e 128 caracteres

**201 Created**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "titular@exemplo.com",
  "role": "TITULAR",
  "status": "PENDING_VERIFICATION",
  "emailVerified": false,
  "totpEnabled": false,
  "createdAt": "2026-11-19T22:00:00Z"
}
```

**400** — e-mail inválido, senha fora do tamanho ou role desconhecida
```json
{ "code": "VALIDATION_ERROR", "fieldErrors": { "email": "must be a well-formed email address" } }
```

**409** — e-mail já cadastrado

**curl**
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"titular@exemplo.com","password":"MinhaS3nha!","role":"TITULAR"}' | jq
```

---

## POST /auth/verify-email

Verifica o e-mail com o token recebido.

> **Em desenvolvimento:** o token é exibido no console da aplicação (LoggingEmailSender). Procure por `[EMAIL]` ou `verification` nos logs após o `register`.

**Body**
```json
{ "token": "<token-recebido-por-email>" }
```

**200 OK** — retorna o perfil atualizado com `status: ACTIVE` e `emailVerified: true`

**400** — token expirado ou inválido

**curl**
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/verify-email \
  -H "Content-Type: application/json" \
  -d '{"token":"<token>"}' | jq
```

---

## POST /auth/login

Autentica e retorna par de tokens JWT.

**Body**
```json
{
  "email": "titular@exemplo.com",
  "password": "MinhaS3nha!"
}
```

`totpCode` é opcional — inclua somente se TOTP estiver habilitado.

**200 OK**
```json
{
  "accessToken": "eyJraWQiOiI...",
  "refreshToken": "YfE4qf9Q0xka...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

O `accessToken` expira em **900 segundos (15 min)**. O `refreshToken` expira em **30 dias**.

**401** — credenciais inválidas
```json
{ "code": "INVALID_CREDENTIALS", "message": "E-mail ou senha incorretos" }
```

**401** — conta suspensa

**403** — TOTP habilitado mas `totpCode` ausente
```json
{ "code": "TOTP_REQUIRED" }
```

**curl**
```bash
# Login simples
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"titular@exemplo.com","password":"MinhaS3nha!"}' | jq

# Login com TOTP
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"titular@exemplo.com","password":"MinhaS3nha!","totpCode":"123456"}' | jq
```

> **Conta PENDING_VERIFICATION:** contas com e-mail não verificado conseguem fazer login normalmente. A verificação não é pré-requisito para autenticação.

---

## POST /auth/refresh

Troca o refresh token por um novo par de tokens. O token antigo é **revogado** (rotação obrigatória).

**Body**
```json
{ "refreshToken": "YfE4qf9Q0xka..." }
```

**200 OK** — mesmo schema de `/login`

**401** — token expirado, revogado ou inválido

**curl**
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refresh_token>"}' | jq
```

---

## POST /auth/logout

Revoga o refresh token. Requer `Authorization: Bearer <accessToken>`.

**Body**
```json
{ "refreshToken": "YfE4qf9Q0xka..." }
```

**204 No Content**

**curl**
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{"refreshToken":"<refresh_token>"}' -o /dev/null -w "%{http_code}"
```

---

## POST /auth/password-reset/request

Solicita reset de senha. Sempre retorna 202 — não confirma se o e-mail existe (evita enumeração).

**Body**
```json
{ "email": "titular@exemplo.com" }
```

**202 Accepted**
```json
{ "message": "Se o e-mail existir, enviaremos instruções de redefinição." }
```

> Em desenvolvimento o token de reset aparece no console (LoggingEmailSender). Token expira em **15 min**.

---

## POST /auth/password-reset/confirm

Confirma o reset com o token recebido por e-mail.

**Body**
```json
{
  "resetToken": "<token>",
  "newPassword": "NovaSenha!123"
}
```

**204 No Content**

**400** — token expirado ou senha fora do tamanho mínimo

---

## GET /users/me

Retorna o perfil do usuário autenticado.

**Headers:** `Authorization: Bearer <accessToken>`

**200 OK**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "titular@exemplo.com",
  "role": "TITULAR",
  "status": "ACTIVE",
  "emailVerified": true,
  "totpEnabled": false,
  "createdAt": "2026-11-19T22:00:00Z"
}
```

**curl**
```bash
export TOKEN="eyJraWQiOiI..."
curl -s http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

## POST /users/me/totp/setup

Inicia a configuração do TOTP. Retorna a URI `otpauth://` para escanear com Google Authenticator, Authy ou similar.

**Headers:** `Authorization: Bearer <accessToken>`

**200 OK**
```json
{
  "otpauthUri": "otpauth://totp/Neuro-Gen:titular@exemplo.com?secret=BASE32&issuer=Neuro-Gen",
  "secret": "BASE32SECRET"
}
```

---

## POST /users/me/totp/confirm

Confirma a configuração do TOTP com o primeiro código gerado pelo autenticador. Habilita o 2FA na conta.

**Headers:** `Authorization: Bearer <accessToken>`

**Body**
```json
{ "code": "123456" }
```

**204 No Content**

---

## DELETE /users/me/totp

Desabilita o TOTP. Requer o código atual para confirmar.

**Headers:** `Authorization: Bearer <accessToken>`

**Body**
```json
{ "code": "123456" }
```

**204 No Content**

---

## Fluxo completo com curl (exemplo end-to-end)

```bash
# 1. Registrar
REGISTER=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"eu@exemplo.com","password":"Senha@123","role":"TITULAR"}')
echo $REGISTER | jq .status

# 2. Ver token de verificação no console da app e verificar e-mail
curl -s -X POST http://localhost:8080/api/v1/auth/verify-email \
  -H "Content-Type: application/json" \
  -d '{"token":"<token-do-console>"}' | jq .status

# 3. Login
LOGIN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"eu@exemplo.com","password":"Senha@123"}')
export ACCESS_TOKEN=$(echo $LOGIN | jq -r .accessToken)
export REFRESH_TOKEN=$(echo $LOGIN | jq -r .refreshToken)

# 4. Perfil
curl -s http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq

# 5. Logout
curl -s -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}" -w "\nHTTP %{http_code}"
```
