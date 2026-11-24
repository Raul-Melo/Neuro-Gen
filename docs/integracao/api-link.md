# API — Link (Vínculo Titular/Apoio)

Base URL: `/api/v1/links`

Todos os endpoints exigem `Authorization: Bearer <accessToken>`.

---

## Regras de acesso

| Operação | Papel exigido |
|---|---|
| Criar convite | `TITULAR` |
| Aceitar convite | `APOIO` |
| Atualizar permissões | `TITULAR` (dono do vínculo) |
| Revogar vínculo | `TITULAR` (dono do vínculo) |
| Listar vínculos | Qualquer (filtra pelo papel) |

---

## POST /links

Cria um convite de vínculo. Retorna o token de convite que deve ser enviado ao Apoio por outro canal.

**201 Created**
```json
{
  "inviteToken": "abc123...",
  "expiresAt": "2025-01-22T14:00:00Z"
}
```

**curl**
```bash
curl -s -X POST http://localhost:8080/api/v1/links \
  -H "Authorization: Bearer $TOKEN_TITULAR" | jq
```

---

## POST /links/accept

Aceita um convite. Deve ser chamado pela conta `APOIO` com o token recebido do Titular.

**Body**
```json
{ "inviteToken": "abc123..." }
```

**200 OK**
```json
{
  "id": "link-uuid",
  "titularId": "uuid-titular",
  "apoioId": "uuid-apoio",
  "status": "ACTIVE",
  "permissions": [],
  "createdAt": "2025-01-19T10:00:00Z"
}
```

**curl**
```bash
curl -s -X POST http://localhost:8080/api/v1/links/accept \
  -H "Authorization: Bearer $TOKEN_APOIO" \
  -H "Content-Type: application/json" \
  -d '{"inviteToken":"abc123..."}' | jq
```

---

## PATCH /links/{linkId}/permissions

Atualiza as permissões granulares do Apoio sobre os módulos do Titular.

Módulos disponíveis: `AGENDA`, `TASKS`, `FOCUS`, `EMOTIONAL_CHECKIN`, `VAULT` _(não recomendado conceder Vault)_.

**Body**
```json
{
  "grant": ["AGENDA", "TASKS"],
  "revoke": ["FOCUS"]
}
```

**200 OK** — retorna o LinkResponse atualizado com a lista `permissions`.

**curl**
```bash
curl -s -X PATCH http://localhost:8080/api/v1/links/$LINK_ID/permissions \
  -H "Authorization: Bearer $TOKEN_TITULAR" \
  -H "Content-Type: application/json" \
  -d '{"grant":["AGENDA","TASKS"],"revoke":[]}' | jq
```

---

## DELETE /links/{linkId}

Revoga o vínculo imediatamente. As sessões do Apoio são invalidadas (RF-01.4).

**204 No Content**

**curl**
```bash
curl -s -X DELETE http://localhost:8080/api/v1/links/$LINK_ID \
  -H "Authorization: Bearer $TOKEN_TITULAR"
```

---

## GET /links

Lista os vínculos do usuário autenticado. Titulares veem vínculos onde são `titular_id`; Apoios veem onde são `apoio_id`.

**200 OK**
```json
[
  {
    "id": "link-uuid",
    "titularId": "uuid",
    "apoioId": "uuid",
    "status": "ACTIVE",
    "permissions": ["AGENDA", "TASKS"],
    "createdAt": "2025-01-19T10:00:00Z"
  }
]
```
