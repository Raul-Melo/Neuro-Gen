# API — Focus (Sessões de Foco)

Base URL: `/api/v1/focus`

Todos os endpoints exigem `Authorization: Bearer <accessToken>`.

---

## Regras de negócio

- Apenas **uma sessão ativa** por usuário por vez.
- Pausa interrompe a contagem de `effectiveSeconds`.
- `complete` ou `cancel` encerram a sessão; após isso a sessão é imutável.

---

## POST /focus/sessions

Inicia uma sessão de foco (Pomodoro configurável).

**Body**
```json
{
  "taskId": "task-uuid-opcional",
  "durationMinutes": 25,
  "breakMinutes": 5
}
```

`taskId` é opcional; vincula a sessão a uma tarefa da Agenda.

**201 Created**
```json
{
  "id": "session-uuid",
  "userId": "user-uuid",
  "taskId": null,
  "plannedMinutes": 25,
  "breakMinutes": 5,
  "status": "RUNNING",
  "startedAt": "2025-01-19T10:00:00Z",
  "effectiveSeconds": 0
}
```

**curl**
```bash
curl -s -X POST http://localhost:8080/api/v1/focus/sessions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"durationMinutes":25,"breakMinutes":5}' | jq
```

---

## GET /focus/sessions/current

Retorna a sessão em andamento, se houver.

**200 OK** — `FocusSessionResult`

**204 No Content** — nenhuma sessão ativa.

---

## POST /focus/sessions/{id}/pause

Pausa a sessão ativa.

**200 OK** — sessão com `status: PAUSED` e `pausedAt` registrado.

---

## POST /focus/sessions/{id}/resume

Retoma a sessão pausada.

**200 OK** — sessão com `status: RUNNING`.

---

## POST /focus/sessions/{id}/complete

Conclui a sessão e registra o tempo efetivo total.

**200 OK**
```json
{
  "id": "session-uuid",
  "status": "COMPLETED",
  "endedAt": "2025-01-19T10:25:00Z",
  "effectiveSeconds": 1500
}
```

---

## POST /focus/sessions/{id}/cancel

Cancela a sessão sem registrar como concluída.

**200 OK** — sessão com `status: CANCELLED`.

---

## GET /focus/sessions?from=&to=

Histórico de sessões no período informado.

**Params:** `from` e `to` em ISO 8601.

**200 OK** — lista de `FocusSessionResult`.

**curl**
```bash
curl -s "http://localhost:8080/api/v1/focus/sessions?from=2025-01-01T00:00:00Z&to=2025-01-31T23:59:59Z" \
  -H "Authorization: Bearer $TOKEN" | jq
```
