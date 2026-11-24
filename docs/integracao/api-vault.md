# API — Vault (Cofre Zero-Knowledge)

Base URL: `/api/v1/vault`

Todos os endpoints exigem `Authorization: Bearer <accessToken>`.

> **Zero-knowledge:** a aplicação nunca conhece o conteúdo plaintext. O cliente cifra com a senha mestra localmente (Argon2 → AES-256-GCM) e envia apenas o ciphertext. O servidor armazena bytes opacos.

> **Importante:** o papel `APOIO` é **estruturalmente bloqueado** em todos os endpoints `/vault/**` antes de chegar ao controller (RF-08.5).

---

## Fluxo completo do Vault

```
1. POST /vault/setup    → configura a senha mestra (uma vez)
2. POST /vault/open     → abre o cofre → retorna vaultSessionToken
3. Use X-Vault-Session: <vaultSessionToken> nas operações de entrada
4. POST /vault/lock     → bloqueia (invalida a sessão)
```

A sessão do Vault expira automaticamente após **5 minutos de inatividade** (`vault.session.ttl-seconds`).

---

## POST /vault/setup

Configura o Cofre. Pode ser chamado apenas uma vez por usuário.

**Body**
```json
{
  "argon2Params": "m=65536,t=3,p=4",
  "masterPasswordHash": "<hash-derivado-localmente-com-argon2>"
}
```

**201 Created**
```json
{
  "recoveryKey": "AAAA-BBBB-CCCC-DDDD-EEEE",
  "configuredAt": "2025-01-19T10:00:00Z"
}
```

> Guarde a `recoveryKey` em local seguro. Ela é exibida **uma única vez**.

---

## POST /vault/open

Abre o Cofre com a senha mestra. Retorna um token de sessão de 5 min.

**Body**
```json
{ "masterPasswordHash": "<hash>" }
```

**200 OK**
```json
{
  "vaultSessionToken": "vs_abc...",
  "expiresAt": "2025-01-19T10:05:00Z"
}
```

**curl**
```bash
curl -s -X POST http://localhost:8080/api/v1/vault/open \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"masterPasswordHash":"<hash>"}' | jq
```

---

## POST /vault/lock

Bloqueia o Cofre (invalida a sessão imediatamente).

**Headers:** `X-Vault-Session: <vaultSessionToken>`

**204 No Content**

---

## POST /vault/recover

Redefine a senha mestra usando a chave de recuperação (RF-08.11).

**Body**
```json
{
  "recoveryKey": "AAAA-BBBB-CCCC-DDDD-EEEE",
  "newMasterPasswordHash": "<novo-hash>"
}
```

**204 No Content**

---

## POST /vault/entries

Cria uma entrada cifrada no Cofre.

**Headers:**
- `Authorization: Bearer <accessToken>`
- `X-Vault-Session: <vaultSessionToken>`

**Body**
```json
{
  "label": "Senha do banco",
  "encryptedPayload": "<base64-do-ciphertext-cifrado-pelo-cliente>",
  "entryType": "CREDENTIAL"
}
```

`entryType` aceita: `CREDENTIAL`, `NOTE`, `DOCUMENT`, `HEALTH`, `OTHER`.

**201 Created**
```json
{
  "id": "entry-uuid",
  "label": "Senha do banco",
  "entryType": "CREDENTIAL",
  "createdAt": "2025-01-19T10:01:00Z"
}
```

---

## GET /vault/entries

Lista as entradas (somente metadados — ciphertext nunca é retornado na listagem).

**Headers:** `X-Vault-Session: <vaultSessionToken>`

**200 OK**
```json
[
  {
    "id": "entry-uuid",
    "label": "Senha do banco",
    "entryType": "CREDENTIAL",
    "createdAt": "2025-01-19T10:01:00Z",
    "updatedAt": "2025-01-19T10:01:00Z"
  }
]
```

---

## PUT /vault/entries/{id}

Atualiza label e/ou ciphertext de uma entrada.

**Headers:** `X-Vault-Session: <vaultSessionToken>`

**Body**
```json
{
  "label": "Senha do banco (atualizado)",
  "encryptedPayload": "<novo-ciphertext>"
}
```

**200 OK** — retorna o entry atualizado.

---

## DELETE /vault/entries/{id}

Exclui uma entrada cifrada.

**Headers:** `X-Vault-Session: <vaultSessionToken>`

**204 No Content**
