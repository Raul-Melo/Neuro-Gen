# API — Vault (Cofre Zero-Knowledge)

Base URL: `/api/v1/vault`

Todos os endpoints exigem `Authorization: Bearer <accessToken>`.

> **Zero-knowledge:** a aplicação nunca conhece o conteúdo plaintext. O cliente cifra com a senha mestra localmente (Argon2 → AES-256-GCM) e envia apenas o ciphertext. O servidor armazena bytes opacos — sem capacidade de decifragem.

> **Restrição estrutural:** o papel `APOIO` é **bloqueado em todos os endpoints `/vault/**`** antes de chegar ao controller (RF-08.5). Não é possível delegar acesso ao Cofre via permissões de vínculo.

---

## Sumário de endpoints

| Método | Path | Requer sessão | Descrição |
|--------|------|---------------|-----------|
| POST | `/vault/setup` | Não | Configura cofre (uma vez por conta) |
| POST | `/vault/open` | Não | Abre o cofre — retorna token de sessão |
| POST | `/vault/recover` | Não | Redefine senha mestra com recovery key |
| POST | `/vault/lock` | Sim | Invalida sessão do cofre |
| POST | `/vault/entries` | Sim | Cria entrada cifrada |
| GET | `/vault/entries` | Sim | Lista entradas |
| GET | `/vault/entries/{id}` | Sim | Busca entrada por ID |
| PUT | `/vault/entries/{id}` | Sim | Atualiza entrada cifrada |
| DELETE | `/vault/entries/{id}` | Sim | Exclui entrada |

---

## Fluxo de acesso

```
1. POST /vault/setup    → configura uma vez (retorna recovery key — exibida uma única vez)
2. POST /vault/open     → autentica com senha mestra → retorna X-Vault-Session token
3. Operações com X-Vault-Session: <token> em todas as chamadas de /vault/entries/**
4. POST /vault/lock     → invalida a sessão ao final do uso
```

A sessão do Vault expira automaticamente após **5 minutos de inatividade** (`vault.session.ttl-seconds`).

---

## POST /vault/setup

Configura o Cofre. Pode ser chamado **apenas uma vez** por conta.

**Body**
```json
{
  "argon2Params": "m=65536,t=3,p=4",
  "masterPasswordHash": "<hash-derivado-localmente-com-argon2>"
}
```

> O hash da senha mestra deve ser derivado **pelo cliente** usando Argon2 com os parâmetros informados. O servidor nunca recebe a senha em texto claro.

**201 Created**
```json
{
  "recoveryKey": "AAAA-BBBB-CCCC-DDDD-EEEE-FFFF"
}
```

> ⚠️ Guarde a `recoveryKey` em local seguro. Ela é exibida **uma única vez** e não pode ser recuperada pelo servidor. Sem ela, uma senha mestra esquecida resulta em perda permanente dos dados.

**409** — Cofre já configurado para este usuário

```bash
curl -s -X POST http://localhost:8080/api/v1/vault/setup \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"argon2Params":"m=65536,t=3,p=4","masterPasswordHash":"<hash>"}' | jq
```

---

## POST /vault/open

Abre o Cofre com a senha mestra. Retorna um token de sessão válido por 5 minutos.

**Body**
```json
{ "masterPasswordHash": "<hash>" }
```

**200 OK**
```json
{
  "vaultSessionToken": "vs_abc123...",
  "expiresInSeconds": 300
}
```

**403** — senha mestra incorreta (registra falha na auditoria)  
**422** — Cofre não configurado

```bash
curl -s -X POST http://localhost:8080/api/v1/vault/open \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"masterPasswordHash":"<hash>"}' | jq
```

---

## POST /vault/recover

Redefine a senha mestra usando a chave de recuperação (RF-08.11). As entradas cifradas **não são afetadas** (zero-knowledge — o servidor não as recifra).

> ⚠️ Após a recuperação, o cliente deve **decifrar e recifrar** todas as entradas localmente com a nova senha mestra, e então atualizá-las via `PUT /vault/entries/{id}`.

**Body**
```json
{
  "recoveryKey": "AAAA-BBBB-CCCC-DDDD-EEEE-FFFF",
  "newMasterPasswordHash": "<novo-hash>"
}
```

**204 No Content**  
**403** — recovery key incorreta  
**422** — Cofre não configurado

---

## POST /vault/lock

Invalida o token de sessão imediatamente. Recomendado ao término de cada uso.

**Headers:** `X-Vault-Session: <vaultSessionToken>`

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

`entryType` aceita: `CREDENTIAL`, `NOTE`, `DOCUMENT`, `HEALTH`, `OTHER`

**201 Created**
```json
{
  "id": "entry-uuid",
  "label": "Senha do banco",
  "entryType": "CREDENTIAL",
  "encryptedPayload": "<ciphertext>",
  "createdAt": "2026-11-26T10:01:00Z",
  "updatedAt": "2026-11-26T10:01:00Z"
}
```

**403** — sessão inválida ou expirada

```bash
curl -s -X POST http://localhost:8080/api/v1/vault/entries \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Vault-Session: $VAULT_SESSION" \
  -H "Content-Type: application/json" \
  -d '{
    "label":"Senha do banco",
    "encryptedPayload":"<ciphertext>",
    "entryType":"CREDENTIAL"
  }' | jq
```

---

## GET /vault/entries/{id}

Busca uma entrada cifrada por ID.

**Headers:** `X-Vault-Session: <vaultSessionToken>`

**200 OK** — `VaultEntryResult` com `encryptedPayload`  
**403** — sessão inválida ou entrada não pertence ao usuário  
**404** — entrada não encontrada

```bash
curl -s http://localhost:8080/api/v1/vault/entries/$ENTRY_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Vault-Session: $VAULT_SESSION" | jq
```

---

## GET /vault/entries

Lista todas as entradas do usuário.

**Headers:** `X-Vault-Session: <vaultSessionToken>`

**200 OK**
```json
[
  {
    "id": "entry-uuid",
    "label": "Senha do banco",
    "entryType": "CREDENTIAL",
    "encryptedPayload": "<ciphertext>",
    "createdAt": "2026-11-26T10:01:00Z",
    "updatedAt": "2026-11-26T10:01:00Z"
  }
]
```

```bash
curl -s http://localhost:8080/api/v1/vault/entries \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Vault-Session: $VAULT_SESSION" | jq
```

---

## PUT /vault/entries/{id}

Atualiza label e/ou ciphertext de uma entrada. O novo payload deve ser cifrado pelo cliente.

**Headers:** `X-Vault-Session: <vaultSessionToken>`

**Body**
```json
{
  "label": "Senha do banco (atualizada)",
  "encryptedPayload": "<novo-ciphertext>"
}
```

**200 OK** — `VaultEntryResult` atualizado  
**403** — sessão inválida ou entrada não pertence ao usuário  
**404** — entrada não encontrada

```bash
curl -s -X PUT http://localhost:8080/api/v1/vault/entries/$ENTRY_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Vault-Session: $VAULT_SESSION" \
  -H "Content-Type: application/json" \
  -d '{"label":"Atualizado","encryptedPayload":"<novo-ciphertext>"}' | jq
```

---

## DELETE /vault/entries/{id}

Exclui permanentemente uma entrada. O ciphertext é descartado sem possibilidade de recuperação.

**Headers:** `X-Vault-Session: <vaultSessionToken>`

**204 No Content**  
**403** — sessão inválida ou entrada não pertence ao usuário  
**404** — entrada não encontrada

```bash
curl -s -X DELETE http://localhost:8080/api/v1/vault/entries/$ENTRY_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Vault-Session: $VAULT_SESSION" -w "%{http_code}"
```

---

## Fluxo completo com curl

```bash
export TOKEN="<access_token>"

# 1. Configurar cofre
SETUP=$(curl -s -X POST http://localhost:8080/api/v1/vault/setup \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"argon2Params":"m=65536,t=3,p=4","masterPasswordHash":"<hash>"}')
echo "Recovery key: $(echo $SETUP | jq -r .recoveryKey)"

# 2. Abrir cofre
OPEN=$(curl -s -X POST http://localhost:8080/api/v1/vault/open \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"masterPasswordHash":"<hash>"}')
export VAULT_SESSION=$(echo $OPEN | jq -r .vaultSessionToken)

# 3. Criar entrada
ENTRY=$(curl -s -X POST http://localhost:8080/api/v1/vault/entries \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Vault-Session: $VAULT_SESSION" \
  -H "Content-Type: application/json" \
  -d '{"label":"Senha do banco","encryptedPayload":"<ciphertext>","entryType":"CREDENTIAL"}')
export ENTRY_ID=$(echo $ENTRY | jq -r .id)

# 4. Buscar entrada por ID
curl -s http://localhost:8080/api/v1/vault/entries/$ENTRY_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Vault-Session: $VAULT_SESSION" | jq .label

# 5. Listar entradas
curl -s http://localhost:8080/api/v1/vault/entries \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Vault-Session: $VAULT_SESSION" | jq 'length'

# 6. Atualizar entrada
curl -s -X PUT http://localhost:8080/api/v1/vault/entries/$ENTRY_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Vault-Session: $VAULT_SESSION" \
  -H "Content-Type: application/json" \
  -d '{"label":"Senha do banco v2","encryptedPayload":"<novo-ciphertext>"}' | jq .label

# 7. Excluir entrada
curl -s -X DELETE http://localhost:8080/api/v1/vault/entries/$ENTRY_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Vault-Session: $VAULT_SESSION" -w "\nHTTP %{http_code}"

# 8. Bloquear cofre
curl -s -X POST http://localhost:8080/api/v1/vault/lock \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Vault-Session: $VAULT_SESSION" -w "\nHTTP %{http_code}"
```
