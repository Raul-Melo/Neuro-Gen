# API — Focus (Sessões de Foco)

Base URL: `/api/v1/focus`

Todos os endpoints exigem `Authorization: Bearer <accessToken>`.

---

## Sumário de endpoints

| Método | Path | Descrição |
|--------|------|-----------|
| POST | `/focus/sessions` | Inicia sessão de foco |
| GET | `/focus/sessions` | Histórico por período |
| GET | `/focus/sessions/current` | Sessão em andamento |
| GET | `/focus/sessions/{id}` | Busca sessão por ID |
| DELETE | `/focus/sessions/{id}` | Remove sessão do histórico |
| POST | `/focus/sessions/{id}/pause` | Pausa a sessão |
| POST | `/focus/sessions/{id}/resume` | Retoma a sessão |
| POST | `/focus/sessions/{id}/complete` | Conclui a sessão |
| POST | `/focus/sessions/{id}/cancel` | Cancela a sessão |

---

## Regras de negócio

- Apenas **uma sessão ativa** (RUNNING ou PAUSED) por usuário por vez.
- `pause` e `resume` cronometram intervalos — o `effectiveSeconds` exclui o tempo pausado.
- `complete` ou `cancel` encerram a sessão; após isso a sessão é imutável.
- Sessões encerradas (COMPLETED ou CANCELLED) ficam no histórico e podem ser removidas com `DELETE`.
- Ao completar, um evento de domínio `FocusSessionEnded` é emitido (integração com Agenda).

---

## Esquema FocusSessionResult

```json
{
  "id": "session-uuid",
  "userId": "user-uuid",
  "taskId": "task-uuid-ou-null",
  "plannedMinutes": 25,
  "breakMinutes": 5,
  "status": "RUNNING",
  "startedAt": "2026-11-26T10:00:00Z",
  "endedAt": null,
  "effectiveSeconds": 0,
  "pauses": []
}
```

| Campo | Descrição |
|-------|-----------|
| `status` | `RUNNING` → `PAUSED` → `RUNNING` → `COMPLETED` \| `CANCELLED` |
| `effectiveSeconds` | Tempo líquido (descontando pausas) |
| `pauses` | Lista de `{pausedAt, resumedAt}` |

---

## POST /focus/sessions

Inicia uma sessão de foco.

**Body**
```json
{
  "taskId": "task-uuid-opcional",
  "durationMinutes": 25,
  "breakMinutes": 5
}
```

**201 Created** — `FocusSessionResult` com `status: RUNNING`  
**409** — já existe uma sessão ativa

**curl**
```bash
curl -s -X POST http://localhost:8080/api/v1/focus/sessions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"durationMinutes":25,"breakMinutes":5}' | jq
```

---

## GET /focus/sessions/{id}

Busca uma sessão específica por ID.

**200 OK** — `FocusSessionResult`  
**403** — sessão não pertence ao usuário autenticado  
**404** — sessão não encontrada

**curl**
```bash
curl -s http://localhost:8080/api/v1/focus/sessions/$SESSION_ID \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

## DELETE /focus/sessions/{id}

Remove uma sessão permanentemente do histórico.

**204 No Content**  
**403** — sessão não pertence ao usuário  
**404** — sessão não encontrada

**curl**
```bash
curl -s -X DELETE http://localhost:8080/api/v1/focus/sessions/$SESSION_ID \
  -H "Authorization: Bearer $TOKEN" -w "%{http_code}"
```

---

## GET /focus/sessions/current

Retorna a sessão em andamento, se houver.

**200 OK** — `FocusSessionResult`  
**204 No Content** — nenhuma sessão ativa

```bash
curl -s http://localhost:8080/api/v1/focus/sessions/current \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

## POST /focus/sessions/{id}/pause

Pausa a sessão ativa.

**200 OK** — sessão com `status: PAUSED`  
**422** — sessão não está em estado RUNNING

---

## POST /focus/sessions/{id}/resume

Retoma a sessão pausada.

**200 OK** — sessão com `status: RUNNING`  
**422** — sessão não está em estado PAUSED

---

## POST /focus/sessions/{id}/complete

Finaliza a sessão e registra o tempo efetivo total.

**200 OK**
```json
{
  "id": "session-uuid",
  "status": "COMPLETED",
  "endedAt": "2026-11-26T10:25:00Z",
  "effectiveSeconds": 1500
}
```

**422** — sessão já encerrada

---

## POST /focus/sessions/{id}/cancel

Cancela a sessão sem registrar como concluída.

**200 OK** — sessão com `status: CANCELLED`  
**422** — sessão já encerrada

---

## GET /focus/sessions?from=&to=

Histórico de sessões no período informado.

**Query params** (obrigatórios): `from` e `to` em ISO 8601 (`2026-01-01T00:00:00Z`)

**200 OK** — lista de `FocusSessionResult`

```bash
curl -s "http://localhost:8080/api/v1/focus/sessions?from=2026-11-01T00:00:00Z&to=2026-11-30T23:59:59Z" \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

## Fluxo completo com curl

```bash
export TOKEN="<access_token>"

# 1. Iniciar sessão de 25 min
SESSION=$(curl -s -X POST http://localhost:8080/api/v1/focus/sessions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"durationMinutes":25,"breakMinutes":5}')
export SESSION_ID=$(echo $SESSION | jq -r .id)

# 2. Pausar
curl -s -X POST http://localhost:8080/api/v1/focus/sessions/$SESSION_ID/pause \
  -H "Authorization: Bearer $TOKEN" | jq .status

# 3. Retomar
curl -s -X POST http://localhost:8080/api/v1/focus/sessions/$SESSION_ID/resume \
  -H "Authorization: Bearer $TOKEN" | jq .status

# 4. Concluir
curl -s -X POST http://localhost:8080/api/v1/focus/sessions/$SESSION_ID/complete \
  -H "Authorization: Bearer $TOKEN" | jq '{status,effectiveSeconds}'

# 5. Buscar por ID
curl -s http://localhost:8080/api/v1/focus/sessions/$SESSION_ID \
  -H "Authorization: Bearer $TOKEN" | jq

# 6. Remover do histórico
curl -s -X DELETE http://localhost:8080/api/v1/focus/sessions/$SESSION_ID \
  -H "Authorization: Bearer $TOKEN" -w "\nHTTP %{http_code}"
```
