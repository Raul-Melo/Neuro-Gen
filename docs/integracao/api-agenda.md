# API — Agenda (Eventos e Tarefas)

Base URLs:
- Eventos: `/api/v1/events`
- Tarefas: `/api/v1/tasks`

Todos os endpoints exigem `Authorization: Bearer <accessToken>`.

---

## EVENTOS

### POST /events

Cria um evento com hora fixa, recorrência opcional e lembretes.

**Body**
```json
{
  "title": "Consulta médica",
  "description": "Clínica central",
  "startAt": "2025-01-25T14:00:00Z",
  "endAt":   "2025-01-25T15:00:00Z",
  "type": "APPOINTMENT",
  "rrule": null,
  "categoryId": null,
  "sharedWithSupport": false,
  "reminders": [
    { "minutesBefore": 30, "persistent": true }
  ]
}
```

`type` aceita: `APPOINTMENT`, `TASK`, `PERSONAL`, `REMINDER`.

**201 Created** — retorna `EventResult` com id, título, período e lista de lembretes.

**curl**
```bash
curl -s -X POST http://localhost:8080/api/v1/events \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title":"Consulta médica",
    "startAt":"2025-01-25T14:00:00Z",
    "endAt":"2025-01-25T15:00:00Z",
    "type":"APPOINTMENT"
  }' | jq
```

---

### PUT /events/{id}/reschedule

Reagenda um evento. Para eventos recorrentes, informe o escopo.

**Body**
```json
{
  "startAt": "2025-01-26T14:00:00Z",
  "endAt":   "2025-01-26T15:00:00Z",
  "scope": "THIS_OCCURRENCE"
}
```

`scope` aceita: `THIS_OCCURRENCE`, `THIS_AND_FOLLOWING`, `ALL`.

**200 OK** — evento atualizado.

---

### POST /events/{id}/cancel

Cancela um evento (muda status para `CANCELLED`).

**Query:** `?scope=THIS_OCCURRENCE` (padrão)

**204 No Content**

---

### DELETE /events/{id}

Remove o evento permanentemente.

**204 No Content**

---

### GET /events?from=&to=

Lista eventos do usuário em um intervalo de datas (visão de calendário).

**Params:** `from` e `to` em formato ISO 8601 (`2025-01-01T00:00:00Z`)

**200 OK** — lista de `EventResult`.

**curl**
```bash
curl -s "http://localhost:8080/api/v1/events?from=2025-01-01T00:00:00Z&to=2025-01-31T23:59:59Z" \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

## TAREFAS

### POST /tasks

Cria uma tarefa com subtarefas e lembretes opcionais.

**Body**
```json
{
  "title": "Preparar relatório",
  "description": "Relatório mensal de humor",
  "priority": "HIGH",
  "dueDate": "2025-01-30",
  "categoryId": null,
  "sharedWithSupport": false,
  "subtasks": [
    { "title": "Coletar dados", "position": 1 },
    { "title": "Revisar", "position": 2 }
  ],
  "reminders": []
}
```

`priority` aceita: `LOW`, `MEDIUM`, `HIGH`, `URGENT`.

**201 Created**

---

### PATCH /tasks/{id}/status

Atualiza o status de uma tarefa.

**Body**
```json
{ "status": "IN_PROGRESS" }
```

`status` aceita: `PENDING`, `IN_PROGRESS`, `DONE`, `CANCELLED`.

**200 OK**

---

### POST /tasks/{id}/subtasks

Adiciona uma subtarefa.

**Body**
```json
{ "title": "Nova subtarefa", "position": 3 }
```

**201 Created**

---

### PATCH /tasks/{taskId}/subtasks/{subtaskId}/done

Marca uma subtarefa como concluída.

**204 No Content**

---

### GET /tasks?status=&dueBefore=

Lista tarefas do usuário com filtro opcional.

**Params opcionais:** `status` (PENDING | IN_PROGRESS | DONE | CANCELLED), `dueBefore` (data ISO)

**200 OK** — lista de `TaskResult` com subtarefas e lembretes.

---

### DELETE /tasks/{id}

Arquiva a tarefa (soft-delete via `archived_at`).

**204 No Content**
