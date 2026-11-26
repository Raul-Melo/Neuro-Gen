# API — Agenda (Eventos e Tarefas)

Base URLs:
- Eventos: `/api/v1/events`
- Tarefas: `/api/v1/tasks`

Todos os endpoints exigem `Authorization: Bearer <accessToken>`.

---

## Sumário de endpoints — Eventos

| Método | Path | Descrição |
|--------|------|-----------|
| POST | `/events` | Cria evento |
| GET | `/events` | Lista eventos por período |
| GET | `/events/{id}` | Busca evento por ID |
| PUT | `/events/{id}` | Atualiza detalhes do evento |
| PUT | `/events/{id}/reschedule` | Reagenda evento |
| POST | `/events/{id}/cancel` | Cancela evento |
| DELETE | `/events/{id}` | Remove evento |
| POST | `/events/{id}/reminders` | Adiciona lembrete |
| DELETE | `/events/{id}/reminders/{reminderId}` | Remove lembrete |

## Sumário de endpoints — Tarefas

| Método | Path | Descrição |
|--------|------|-----------|
| POST | `/tasks` | Cria tarefa |
| GET | `/tasks` | Lista tarefas |
| GET | `/tasks/{id}` | Busca tarefa por ID |
| PUT | `/tasks/{id}` | Atualiza detalhes da tarefa |
| DELETE | `/tasks/{id}` | Arquiva tarefa (soft-delete) |
| PATCH | `/tasks/{id}/status` | Atualiza status |
| POST | `/tasks/{id}/subtasks` | Adiciona subtarefa |
| PATCH | `/tasks/{taskId}/subtasks/{subtaskId}/done` | Marca subtarefa como concluída |
| DELETE | `/tasks/{id}/subtasks/{subtaskId}` | Remove subtarefa |
| POST | `/tasks/{id}/reminders` | Adiciona lembrete |
| DELETE | `/tasks/{id}/reminders/{reminderId}` | Remove lembrete |

---

## EVENTOS

### POST /events

Cria um evento com hora fixa, recorrência opcional e lembretes.

**Body**
```json
{
  "title": "Consulta médica",
  "description": "Clínica central — Dr. Silva",
  "startAt": "2026-12-01T14:00:00Z",
  "endAt":   "2026-12-01T15:00:00Z",
  "type": "APPOINTMENT",
  "rrule": null,
  "categoryId": null,
  "sharedWithSupport": false,
  "reminders": [
    { "minutesBefore": 30, "persistent": true }
  ]
}
```

| Campo | Valores | Descrição |
|-------|---------|-----------|
| `type` | `APPOINTMENT`, `PERSONAL`, `ROUTINE` | Tipo do evento |
| `rrule` | string RFC 5545 | Recorrência (ex: `FREQ=WEEKLY;BYDAY=MO,WE,FR`) |
| `sharedWithSupport` | boolean | Visível para Apoio com permissão AGENDA |

**201 Created** — retorna `EventResult` com id, título, período e lista de lembretes.

```bash
curl -s -X POST http://localhost:8080/api/v1/events \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title":"Consulta médica",
    "startAt":"2026-12-01T14:00:00Z",
    "endAt":"2026-12-01T15:00:00Z",
    "type":"APPOINTMENT"
  }' | jq
```

---

### GET /events?from=&to=

Lista eventos do usuário em um intervalo de datas (visão de calendário).

**Query params** (obrigatórios): `from` e `to` em ISO 8601

**200 OK** — lista de `EventResult`

```bash
curl -s "http://localhost:8080/api/v1/events?from=2026-12-01T00:00:00Z&to=2026-12-31T23:59:59Z" \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

### GET /events/{id}

Busca um evento específico por ID.

**200 OK** — `EventResult`  
**403** — evento não pertence ao usuário  
**404** — não encontrado

```bash
curl -s http://localhost:8080/api/v1/events/$EVENT_ID \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

### PUT /events/{id}

Atualiza os detalhes do evento (título, descrição, tipo, categoria, visibilidade para Apoio).

**Body**
```json
{
  "title": "Consulta médica — reagendada",
  "description": "Nova localização: Rua X",
  "type": "APPOINTMENT",
  "categoryId": null,
  "sharedWithSupport": true
}
```

**200 OK** — `EventResult` atualizado  
**403** — evento não pertence ao usuário  
**404** — não encontrado

```bash
curl -s -X PUT http://localhost:8080/api/v1/events/$EVENT_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Título atualizado","type":"APPOINTMENT","sharedWithSupport":false}' | jq
```

---

### PUT /events/{id}/reschedule

Reagenda um evento. Para recorrentes, informe o escopo.

**Body**
```json
{
  "startAt": "2026-12-02T14:00:00Z",
  "endAt":   "2026-12-02T15:00:00Z",
  "scope": "THIS_OCCURRENCE"
}
```

`scope` aceita: `THIS_OCCURRENCE`, `THIS_AND_FOLLOWING`, `ALL`

**200 OK** — evento reagendado

---

### POST /events/{id}/cancel

Cancela um evento (muda status para `CANCELLED`).

**Query params opcionais:** `scope` (padrão: `THIS_OCCURRENCE`)

**204 No Content**

---

### DELETE /events/{id}

Remove o evento permanentemente.

**204 No Content**  
**403** — não pertence ao usuário

---

### POST /events/{id}/reminders

Adiciona um lembrete ao evento.

**Body**
```json
{
  "minutesBefore": 15,
  "persistent": false
}
```

`persistent: true` = lembrete aparece até ser dispensado manualmente.

**201 Created** — `EventResult` atualizado com a lista de lembretes

```bash
curl -s -X POST http://localhost:8080/api/v1/events/$EVENT_ID/reminders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"minutesBefore":30,"persistent":true}' | jq
```

---

### DELETE /events/{id}/reminders/{reminderId}

Remove um lembrete do evento.

**200 OK** — `EventResult` sem o lembrete removido

---

## TAREFAS

### POST /tasks

Cria uma tarefa com subtarefas e lembretes opcionais.

**Body**
```json
{
  "title": "Preparar relatório mensal",
  "description": "Relatório de humor e foco de novembro",
  "priority": "HIGH",
  "dueDate": "2026-11-30",
  "categoryId": null,
  "sharedWithSupport": false,
  "subtasks": [
    { "title": "Coletar dados de check-ins", "position": 1 },
    { "title": "Revisar tendências de foco", "position": 2 },
    { "title": "Redigir relatório", "position": 3 }
  ],
  "reminders": [
    { "minutesBefore": 1440, "persistent": false }
  ]
}
```

`priority` aceita: `LOW`, `MEDIUM`, `HIGH`, `URGENT`

**201 Created** — `TaskResult` com id, subtarefas e lembretes

---

### GET /tasks?status=&dueBefore=

Lista tarefas do usuário com filtros opcionais.

**Query params opcionais:**

| Param | Valores | Descrição |
|-------|---------|-----------|
| `status` | `PENDING`, `IN_PROGRESS`, `DONE`, `CANCELLED` | Filtrar por status |
| `dueBefore` | data ISO (`YYYY-MM-DD`) | Tarefas com prazo até a data |

**200 OK** — lista de `TaskResult` com subtarefas e lembretes

```bash
curl -s "http://localhost:8080/api/v1/tasks?status=PENDING" \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

### GET /tasks/{id}

Busca uma tarefa específica por ID.

**200 OK** — `TaskResult` completo com subtarefas e lembretes  
**403** — tarefa não pertence ao usuário  
**404** — não encontrada

```bash
curl -s http://localhost:8080/api/v1/tasks/$TASK_ID \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

### PUT /tasks/{id}

Atualiza os detalhes da tarefa (título, descrição, prioridade, prazo, categoria, visibilidade).

**Body**
```json
{
  "title": "Preparar relatório mensal — revisado",
  "description": "Incluir dados de dezembro",
  "priority": "URGENT",
  "dueDate": "2026-12-05",
  "categoryId": null,
  "sharedWithSupport": false
}
```

**200 OK** — `TaskResult` atualizado  
**403** — tarefa não pertence ao usuário  
**404** — não encontrada

```bash
curl -s -X PUT http://localhost:8080/api/v1/tasks/$TASK_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Título atualizado","priority":"URGENT","dueDate":"2026-12-05"}' | jq
```

---

### DELETE /tasks/{id}

Arquiva a tarefa (soft-delete via `archived_at`). Não remove fisicamente do banco.

**204 No Content**

---

### PATCH /tasks/{id}/status

Atualiza o status de uma tarefa.

**Body**
```json
{ "status": "IN_PROGRESS" }
```

`status` aceita: `PENDING`, `IN_PROGRESS`, `DONE`, `CANCELLED`

**200 OK**

```bash
curl -s -X PATCH http://localhost:8080/api/v1/tasks/$TASK_ID/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS"}' | jq .status
```

---

### POST /tasks/{id}/subtasks

Adiciona uma subtarefa.

**Body**
```json
{ "title": "Nova subtarefa", "position": 4 }
```

**201 Created** — `TaskResult` atualizado com a nova subtarefa

---

### PATCH /tasks/{taskId}/subtasks/{subtaskId}/done

Marca uma subtarefa como concluída.

**204 No Content**

---

### DELETE /tasks/{id}/subtasks/{subtaskId}

Remove uma subtarefa permanentemente.

**200 OK** — `TaskResult` sem a subtarefa removida

---

### POST /tasks/{id}/reminders

Adiciona um lembrete à tarefa.

**Body**
```json
{
  "minutesBefore": 60,
  "persistent": false
}
```

**201 Created** — `TaskResult` atualizado com o novo lembrete

```bash
curl -s -X POST http://localhost:8080/api/v1/tasks/$TASK_ID/reminders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"minutesBefore":60,"persistent":false}' | jq
```

---

### DELETE /tasks/{id}/reminders/{reminderId}

Remove um lembrete da tarefa.

**200 OK** — `TaskResult` sem o lembrete removido

---

## Fluxo completo com curl

```bash
export TOKEN="<access_token>"

# 1. Criar evento
EVENT=$(curl -s -X POST http://localhost:8080/api/v1/events \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title":"Reunião semanal",
    "startAt":"2026-12-02T10:00:00Z",
    "endAt":"2026-12-02T11:00:00Z",
    "type":"ROUTINE"
  }')
export EVENT_ID=$(echo $EVENT | jq -r .id)

# 2. Adicionar lembrete ao evento
curl -s -X POST http://localhost:8080/api/v1/events/$EVENT_ID/reminders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"minutesBefore":15,"persistent":true}' | jq '.reminders | length'

# 3. Buscar evento por ID
curl -s http://localhost:8080/api/v1/events/$EVENT_ID \
  -H "Authorization: Bearer $TOKEN" | jq .title

# 4. Criar tarefa com subtarefas
TASK=$(curl -s -X POST http://localhost:8080/api/v1/tasks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title":"Relatório semanal",
    "priority":"HIGH",
    "dueDate":"2026-12-05",
    "subtasks":[{"title":"Coletar dados","position":1}]
  }')
export TASK_ID=$(echo $TASK | jq -r .id)
export SUBTASK_ID=$(echo $TASK | jq -r '.subtasks[0].id')

# 5. Buscar tarefa por ID
curl -s http://localhost:8080/api/v1/tasks/$TASK_ID \
  -H "Authorization: Bearer $TOKEN" | jq '.title,.priority'

# 6. Atualizar status
curl -s -X PATCH http://localhost:8080/api/v1/tasks/$TASK_ID/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS"}' | jq .status

# 7. Marcar subtarefa como concluída
curl -s -X PATCH http://localhost:8080/api/v1/tasks/$TASK_ID/subtasks/$SUBTASK_ID/done \
  -H "Authorization: Bearer $TOKEN" -w "%{http_code}"

# 8. Listar tarefas pendentes
curl -s "http://localhost:8080/api/v1/tasks?status=IN_PROGRESS" \
  -H "Authorization: Bearer $TOKEN" | jq 'length'
```
