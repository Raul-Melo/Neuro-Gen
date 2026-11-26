# API — Link (Vínculo Titular/Apoio)

Base URL: `/api/v1/links`

Todos os endpoints exigem `Authorization: Bearer <accessToken>`.

---

## Sumário de endpoints

| Método | Path | Papel | Descrição |
|--------|------|-------|-----------|
| POST | `/links` | TITULAR | Cria convite de vínculo |
| POST | `/links/accept` | APOIO | Aceita convite |
| GET | `/links` | Qualquer | Lista vínculos do usuário |
| GET | `/links/{linkId}` | Titular dono ou Apoio participante | Busca vínculo por ID |
| PATCH | `/links/{linkId}/permissions` | TITULAR dono | Atualiza permissões granulares |
| DELETE | `/links/{linkId}` | TITULAR dono | Revoga o vínculo |

---

## Regras de acesso

| Operação | Papel exigido |
|----------|---------------|
| Criar convite | `TITULAR` |
| Aceitar convite | `APOIO` |
| Buscar/Listar vínculos | Titular ou Apoio (filtra pelo papel) |
| Atualizar permissões | `TITULAR` dono do vínculo |
| Revogar vínculo | `TITULAR` dono do vínculo |

---

## Esquema LinkResponse

```json
{
  "id": "link-uuid",
  "titularId": "uuid-titular",
  "apoioId": "uuid-apoio",
  "status": "ACTIVE",
  "permissions": ["AGENDA", "FOCUS"],
  "createdAt": "2026-11-26T10:00:00Z"
}
```

`status` aceita: `PENDING` (convite emitido), `ACTIVE` (aceito), `REVOKED`, `EXPIRED`

---

## POST /links

Cria um convite de vínculo. O token gerado deve ser enviado ao Apoio por outro canal (mensagem, e-mail, etc.).

**201 Created**
```json
{
  "linkId": "link-uuid",
  "inviteToken": "abc123def456...",
  "expiresAt": "2026-11-29T10:00:00Z"
}
```

O token de convite expira em **72 horas** e é de **uso único**.

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
{ "inviteToken": "abc123def456..." }
```

**200 OK** — retorna `LinkResponse` com `status: ACTIVE` e `permissions: []`

```bash
curl -s -X POST http://localhost:8080/api/v1/links/accept \
  -H "Authorization: Bearer $TOKEN_APOIO" \
  -H "Content-Type: application/json" \
  -d '{"inviteToken":"abc123def456..."}' | jq
```

**404** — token inválido ou expirado  
**409** — token já utilizado ou vínculo já existe entre os usuários

---

## GET /links

Lista os vínculos do usuário autenticado. A visão é determinada automaticamente pelo papel:

- **Titular** → vê vínculos onde é dono (`titular_id`)
- **Apoio** → vê vínculos onde participa (`apoio_id`)

**200 OK**
```json
[
  {
    "id": "link-uuid",
    "titularId": "uuid",
    "apoioId": "uuid",
    "status": "ACTIVE",
    "permissions": ["AGENDA", "TASKS"],
    "createdAt": "2026-11-26T10:00:00Z"
  }
]
```

```bash
curl -s http://localhost:8080/api/v1/links \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

## GET /links/{linkId}

Busca um vínculo específico por ID. Acessível pelo Titular dono **e** pelo Apoio participante.

**200 OK** — `LinkResponse`  
**403** — vínculo não pertence ao usuário autenticado  
**404** — vínculo não encontrado

```bash
curl -s http://localhost:8080/api/v1/links/$LINK_ID \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

## PATCH /links/{linkId}/permissions

Atualiza as permissões granulares do Apoio sobre os módulos do Titular.

**Módulos disponíveis:**

| Permissão | Acesso concedido |
|-----------|-----------------|
| `AGENDA` | Leitura de eventos do Titular |
| `TASKS` | Leitura de tarefas do Titular |
| `FOCUS` | Leitura do histórico de foco |
| `EMOTIONAL_CHECKIN` | Leitura da tendência emocional (scores agregados, nunca notas/diário) |

> **Vault não aparece nesta lista.** O acesso ao Cofre é bloqueado **estruturalmente** para o papel APOIO antes mesmo de chegar ao controller (RF-08.5) — não é possível conceder via permissões.

**Body**
```json
{
  "grant": ["AGENDA", "TASKS"],
  "revoke": ["FOCUS"]
}
```

Ambas as listas podem ser vazias `[]`. As permissões não listadas em nenhuma das listas permanecem inalteradas.

**200 OK** — `LinkResponse` atualizado com a nova lista `permissions`

```bash
curl -s -X PATCH http://localhost:8080/api/v1/links/$LINK_ID/permissions \
  -H "Authorization: Bearer $TOKEN_TITULAR" \
  -H "Content-Type: application/json" \
  -d '{"grant":["AGENDA","TASKS"],"revoke":[]}' | jq
```

---

## DELETE /links/{linkId}

Revoga o vínculo. Efeito **imediato**: o Apoio perde acesso a todos os dados delegados instantaneamente. Registra evento de auditoria.

**204 No Content**  
**403** — apenas o Titular dono pode revogar

```bash
curl -s -X DELETE http://localhost:8080/api/v1/links/$LINK_ID \
  -H "Authorization: Bearer $TOKEN_TITULAR" -w "%{http_code}"
```

---

## Fluxo completo Titular → Apoio

```bash
export TOKEN_TITULAR="<access_token_titular>"
export TOKEN_APOIO="<access_token_apoio>"

# 1. Titular cria convite
INVITE=$(curl -s -X POST http://localhost:8080/api/v1/links \
  -H "Authorization: Bearer $TOKEN_TITULAR")
export LINK_ID=$(echo $INVITE | jq -r .linkId)
export INVITE_TOKEN=$(echo $INVITE | jq -r .inviteToken)
echo "Token de convite: $INVITE_TOKEN"

# 2. Apoio aceita convite (token enviado por outro canal)
curl -s -X POST http://localhost:8080/api/v1/links/accept \
  -H "Authorization: Bearer $TOKEN_APOIO" \
  -H "Content-Type: application/json" \
  -d "{\"inviteToken\":\"$INVITE_TOKEN\"}" | jq .status

# 3. Titular concede permissões
curl -s -X PATCH http://localhost:8080/api/v1/links/$LINK_ID/permissions \
  -H "Authorization: Bearer $TOKEN_TITULAR" \
  -H "Content-Type: application/json" \
  -d '{"grant":["AGENDA","EMOTIONAL_CHECKIN"],"revoke":[]}' | jq .permissions

# 4. Buscar vínculo por ID (qualquer uma das partes)
curl -s http://localhost:8080/api/v1/links/$LINK_ID \
  -H "Authorization: Bearer $TOKEN_TITULAR" | jq

# 5. Revogar
curl -s -X DELETE http://localhost:8080/api/v1/links/$LINK_ID \
  -H "Authorization: Bearer $TOKEN_TITULAR" -w "\nHTTP %{http_code}"
```
