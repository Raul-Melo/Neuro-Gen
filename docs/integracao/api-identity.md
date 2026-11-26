# API — Identity (Autenticação e Perfil)

Base URL: `http://localhost:8080/api/v1`

> **Primeiro uso:** o banco inicia vazio. Chame `POST /auth/register` antes de tentar login.

---

## Sumário de endpoints

| Método | Path | Auth | Descrição |
|--------|------|------|-----------|
| GET | `/dev/verification-token?email=` | Pública | Retorna token de verificação de e-mail |
| POST | `/auth/register` | Pública | Cria conta |
| POST | `/auth/verify-email` | Pública | Verifica e-mail |
| POST | `/auth/login` | Pública | Autentica e retorna tokens |
| POST | `/auth/refresh` | Pública | Renova tokens |
| POST | `/auth/logout` | JWT | Revoga refresh token |
| POST | `/auth/password-reset/request` | Pública | Solicita reset de senha |
| POST | `/auth/password-reset/confirm` | Pública | Confirma reset |
| GET | `/users/me` | JWT | Retorna perfil do usuário |
| PUT | `/users/me` | JWT | Atualiza perfil |
| POST | `/users/me/totp` | JWT | Inicia configuração de 2FA (gera segredo) |
| GET | `/users/me/totp/setup` | JWT | Retorna URI TOTP **uma única vez** |
| POST | `/users/me/totp/confirm` | JWT | Confirma o 2FA com o primeiro código |
| DELETE | `/users/me/totp` | JWT | Desativa o 2FA |

---

## GET /dev/verification-token

Retorna o token de verificação de e-mail para o endereço informado, sem depender de envio por e-mail.

**Query param:** `email` — e-mail de uma conta já registrada

**200 OK**
```json
{
  "email": "titular@exemplo.com",
  "verificationToken": "abc123...",
  "usage": "POST /api/v1/auth/verify-email com body {\"token\": \"<verificationToken>\"}"
}
```

**404** — e-mail não encontrado

> Cada chamada reemite um novo token válido por 24h. Use esse token diretamente em `POST /auth/verify-email`.

```bash
curl -s "http://localhost:8080/api/v1/dev/verification-token?email=titular@exemplo.com" | jq
```

---

## POST /auth/register

Cria uma nova conta. O e-mail fica em estado `PENDING_VERIFICATION` até a verificação.

**Body**
```json
{
  "email": "titular@exemplo.com",
  "password": "MinhaS3nha!",
  "role": "TITULAR",
  "cpf": "529.982.247-25",
  "birthDate": "1990-06-15"
}
```

| Campo | Tipo | Obrigatório | Validação |
|-------|------|-------------|-----------|
| `email` | string | sim | e-mail válido, único |
| `password` | string | sim | 8–128 caracteres |
| `role` | string | sim | `TITULAR` ou `APOIO` |
| `cpf` | string | sim | CPF com ou sem máscara — validado pelo algoritmo brasileiro |
| `birthDate` | string | sim | ISO 8601 `YYYY-MM-DD`, data no passado |

**201 Created**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "titular@exemplo.com",
  "role": "TITULAR",
  "status": "PENDING_VERIFICATION",
  "emailVerified": false,
  "cpf": "529.982.247-25",
  "birthDate": "1990-06-15",
  "hasPendingTotpSetup": false,
  "totpEnabled": false,
  "createdAt": "2026-11-26T10:00:00Z"
}
```

**400** — e-mail inválido, CPF inválido, data futura, senha fora do tamanho ou role desconhecida  
**409** — e-mail já cadastrado

**curl**
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email":"titular@exemplo.com",
    "password":"MinhaS3nha!",
    "role":"TITULAR",
    "cpf":"529.982.247-25",
    "birthDate":"1990-06-15"
  }' | jq
```

---

## POST /auth/verify-email

Verifica o e-mail com o token recebido.

> **Em desenvolvimento:** obtenha o token via `GET /api/v1/dev/verification-token?email=<email>` sem precisar verificar logs.

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

`totpCode` é opcional — inclua somente se o TOTP estiver **confirmado** (`totpEnabled: true`).

```json
{
  "email": "titular@exemplo.com",
  "password": "MinhaS3nha!",
  "totpCode": "123456"
}
```

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

**401** — credenciais inválidas ou conta suspensa  
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

> **Conta PENDING_VERIFICATION:** contas com e-mail não verificado conseguem fazer login normalmente.

---

## POST /auth/refresh

Troca o refresh token por um novo par. O token antigo é **revogado** imediatamente (rotação obrigatória).

**Body**
```json
{ "refreshToken": "YfE4qf9Q0xka..." }
```

**200 OK** — mesmo schema de `/login`  
**401** — token expirado, revogado ou inválido

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

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{"refreshToken":"<refresh_token>"}' -w "%{http_code}"
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

> Em desenvolvimento o token de reset aparece no console. Expira em **15 min**.

---

## POST /auth/password-reset/confirm

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
  "cpf": "529.982.247-25",
  "birthDate": "1990-06-15",
  "hasPendingTotpSetup": false,
  "totpEnabled": false,
  "createdAt": "2026-11-26T10:00:00Z"
}
```

| Campo | Descrição |
|-------|-----------|
| `cpf` | CPF no formato `XXX.XXX.XXX-XX` (mascarado) |
| `birthDate` | Data de nascimento `YYYY-MM-DD` |
| `hasPendingTotpSetup` | `true` quando `POST /me/totp` foi chamado mas o 2FA ainda não foi confirmado |
| `totpEnabled` | `true` quando o 2FA está ativo e confirmado |

```bash
export TOKEN="eyJraWQiOiI..."
curl -s http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

## Fluxo de 2FA (TOTP)

O 2FA segue um fluxo de **três etapas** para evitar que o usuário fique bloqueado sem ter configurado o autenticador:

```
1. POST /users/me/totp          → gera segredo (totpEnabled=false, hasPendingTotpSetup=true)
2. GET  /users/me/totp/setup    → retorna URI + segredo UMA ÚNICA VEZ (use para escanear o QR)
3. POST /users/me/totp/confirm  → valida primeiro código → ativa o 2FA (totpEnabled=true)
```

> **Importante:** o GET de setup é one-time por design. Após a primeira chamada, o endpoint retorna 422. Para gerar um novo segredo, chame `POST /users/me/totp` novamente (só funciona se o 2FA não estiver ativo).

---

### POST /users/me/totp

Gera um novo segredo TOTP pendente. **Não retorna o segredo** — use `GET /users/me/totp/setup` para obtê-lo.

Se o 2FA já estiver ativo (`totpEnabled: true`), retorna **422 TOTP_ALREADY_ENABLED**. Para trocar o segredo, desative primeiro com `DELETE /users/me/totp`.

**Headers:** `Authorization: Bearer <accessToken>`

**204 No Content** — segredo gerado e aguardando visualização

```bash
curl -s -X POST http://localhost:8080/api/v1/users/me/totp \
  -H "Authorization: Bearer $TOKEN" -w "%{http_code}"
```

---

### GET /users/me/totp/setup

Retorna a URI `otpauth://` e o segredo base32 para configurar o autenticador. **Chamada única por sessão de setup** — após retornar, o endpoint rejeita novas chamadas com 422.

**Headers:** `Authorization: Bearer <accessToken>`

**200 OK**
```json
{
  "otpauthUri": "otpauth://totp/Neuro-Gen:titular@exemplo.com?secret=JBSWY3DPEHPK3PXP&issuer=Neuro-Gen",
  "secret": "JBSWY3DPEHPK3PXP"
}
```

**422 TOTP_SETUP_ALREADY_SHOWN** — código já foi exibido; chame `POST /me/totp` para gerar um novo  
**404** — nenhum segredo pendente; chame `POST /me/totp` primeiro

**curl**
```bash
curl -s http://localhost:8080/api/v1/users/me/totp/setup \
  -H "Authorization: Bearer $TOKEN" | jq
```

> **Frontend:** use o campo `otpauthUri` para gerar um QR Code (ex: biblioteca `qrcode` do npm). O usuário escaneia com Google Authenticator, Authy ou similar. Após escanear, passe para a etapa 3.

---

### POST /users/me/totp/confirm

Confirma o 2FA com o primeiro código gerado pelo autenticador. A partir daqui o login exigirá o campo `totpCode`.

**Headers:** `Authorization: Bearer <accessToken>`

**Body**
```json
{ "code": "123456" }
```

**204 No Content**  
**401** — código inválido ou expirado (janela de 30 s)

```bash
curl -s -X POST http://localhost:8080/api/v1/users/me/totp/confirm \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"code":"123456"}' -w "%{http_code}"
```

---

### DELETE /users/me/totp

Desativa o 2FA. Requer o código atual do autenticador.

**Headers:** `Authorization: Bearer <accessToken>`

**Body**
```json
{ "code": "123456" }
```

**204 No Content**  
**401** — código inválido

```bash
curl -s -X DELETE http://localhost:8080/api/v1/users/me/totp \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"code":"123456"}' -w "%{http_code}"
```

---

## Fluxo completo com curl (end-to-end)

```bash
# 1. Registrar
REGISTER=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email":"eu@exemplo.com",
    "password":"Senha@123",
    "role":"TITULAR",
    "cpf":"529.982.247-25",
    "birthDate":"1990-01-15"
  }')
echo $REGISTER | jq .status

# 2. Buscar token de verificação e verificar e-mail
VERIFY_TOKEN=$(curl -s "http://localhost:8080/api/v1/dev/verification-token?email=eu@exemplo.com" | jq -r .verificationToken)
curl -s -X POST http://localhost:8080/api/v1/auth/verify-email \
  -H "Content-Type: application/json" \
  -d "{\"token\":\"$VERIFY_TOKEN\"}" | jq .status

# 3. Login
LOGIN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"eu@exemplo.com","password":"Senha@123"}')
export ACCESS_TOKEN=$(echo $LOGIN | jq -r .accessToken)
export REFRESH_TOKEN=$(echo $LOGIN | jq -r .refreshToken)

# 4. Perfil
curl -s http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq

# 5. Configurar 2FA
curl -s -X POST http://localhost:8080/api/v1/users/me/totp \
  -H "Authorization: Bearer $ACCESS_TOKEN" -w "\nHTTP %{http_code}\n"

SETUP=$(curl -s http://localhost:8080/api/v1/users/me/totp/setup \
  -H "Authorization: Bearer $ACCESS_TOKEN")
echo $SETUP | jq .otpauthUri
# → escaneie o QR com o autenticador, depois:

curl -s -X POST http://localhost:8080/api/v1/users/me/totp/confirm \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"code":"<codigo-do-app>"}' -w "\nHTTP %{http_code}\n"

# 6. Logout
curl -s -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}" -w "\nHTTP %{http_code}"
```
