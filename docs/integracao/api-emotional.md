# API — Emotional (Regulação Emocional)

Base URL: `/api/v1/emotional`

Todos os endpoints exigem `Authorization: Bearer <accessToken>`.

> **Dado sensível (LGPD):** os campos `notes` dos check-ins e o conteúdo do diário são cifrados em repouso com AES-256-GCM na tabela `ng_sensitive`. O servidor decifra apenas ao retornar dados ao próprio usuário. O Apoio **nunca** acessa notas individuais — apenas a tendência agregada (scores médios), e somente se a permissão `EMOTIONAL_CHECKIN` estiver concedida no vínculo.

---

## Sumário de endpoints

| Método | Path | Descrição |
|--------|------|-----------|
| POST | `/emotional/checkins` | Registra check-in do dia (máx. 1/dia) |
| GET | `/emotional/checkins` | Lista check-ins do período |
| GET | `/emotional/checkins/{id}` | Busca check-in por ID |
| DELETE | `/emotional/checkins/{id}` | Remove check-in |
| POST | `/emotional/journal` | Escreve entrada de diário (cifrada) |
| GET | `/emotional/journal` | Lista entradas de diário |
| GET | `/emotional/journal/{id}` | Busca entrada por ID |
| PUT | `/emotional/journal/{id}` | Edita conteúdo da entrada |
| DELETE | `/emotional/journal/{id}` | Remove entrada |
| GET | `/emotional/trend` | Tendência emocional do período |

---

## Escalas

| Campo | Escala | Descrição |
|-------|--------|-----------|
| `moodScore` | 1–5 | 1 = muito difícil, 5 = muito bem |
| `energyLevel` | 1–5 | 1 = sem energia, 5 = muito energizado |
| `moodLevel` | enum | `VERY_LOW`, `LOW`, `NEUTRAL`, `HIGH`, `VERY_HIGH` — derivado do moodScore |

---

## Check-ins

### POST /emotional/checkins

Registra um check-in de humor e energia. **Máximo de 1 check-in por dia** por usuário.

**Body**
```json
{
  "moodScore": 4,
  "energyLevel": 3,
  "notes": "Dia produtivo, foco melhorou no período da tarde."
}
```

| Campo | Tipo | Obrigatório | Validação |
|-------|------|-------------|-----------|
| `moodScore` | int | sim | 1–5 |
| `energyLevel` | int | sim | 1–5 |
| `notes` | string | não | máx. 500 chars, cifrado em repouso |

**201 Created**
```json
{
  "id": "checkin-uuid",
  "moodScore": 4,
  "energyLevel": 3,
  "moodLevel": "HIGH",
  "checkInDate": "2026-11-26",
  "checkInTime": "14:30:00",
  "notes": "Dia produtivo, foco melhorou no período da tarde."
}
```

**400** — scores fora de 1–5  
**409** — já existe um check-in para hoje

```bash
curl -s -X POST http://localhost:8080/api/v1/emotional/checkins \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"moodScore":4,"energyLevel":3,"notes":"Foco bom hoje"}' | jq
```

---

### GET /emotional/checkins?periodDays=

Lista os check-ins dos últimos N dias.

**Query params:** `periodDays` (padrão: `30`)

**200 OK** — lista de `MoodCheckInResult`

```bash
curl -s "http://localhost:8080/api/v1/emotional/checkins?periodDays=7" \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

### GET /emotional/checkins/{id}

Busca um check-in específico por ID.

**200 OK** — `MoodCheckInResult`  
**403** — check-in não pertence ao usuário  
**404** — não encontrado

```bash
curl -s http://localhost:8080/api/v1/emotional/checkins/$CHECKIN_ID \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

### DELETE /emotional/checkins/{id}

Remove um check-in permanentemente.

**204 No Content**  
**403** — check-in não pertence ao usuário  
**404** — não encontrado

```bash
curl -s -X DELETE http://localhost:8080/api/v1/emotional/checkins/$CHECKIN_ID \
  -H "Authorization: Bearer $TOKEN" -w "%{http_code}"
```

---

## Diário Emocional

### POST /emotional/journal

Escreve uma entrada de diário. O conteúdo é cifrado pelo servidor antes de persistir.

**Body**
```json
{
  "content": "Hoje consegui manter o foco por mais de 2 horas sem interrupções. Percebi que a música ambiente ajuda muito.",
  "relatedCheckInId": "checkin-uuid-opcional"
}
```

| Campo | Tipo | Obrigatório | Validação |
|-------|------|-------------|-----------|
| `content` | string | sim | não pode ser vazio, máx. 10 000 chars |
| `relatedCheckInId` | UUID | não | vincula à entrada de check-in do dia |

**201 Created**
```json
{
  "id": "journal-uuid",
  "content": "Hoje consegui manter o foco por mais de 2 horas...",
  "relatedCheckInId": "checkin-uuid",
  "entryDate": "2026-11-26",
  "createdAt": "2026-11-26T14:35:00Z",
  "updatedAt": "2026-11-26T14:35:00Z"
}
```

```bash
curl -s -X POST http://localhost:8080/api/v1/emotional/journal \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content":"Hoje foi um dia difícil mas resiliente."}' | jq
```

---

### GET /emotional/journal

Lista todas as entradas de diário do usuário. Ordenadas por data decrescente. Conteúdo descriptografado.

**200 OK** — lista de `JournalEntryResult`

```bash
curl -s http://localhost:8080/api/v1/emotional/journal \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

### GET /emotional/journal/{id}

Busca uma entrada específica por ID.

**200 OK** — `JournalEntryResult` com conteúdo descriptografado  
**403** — entrada não pertence ao usuário  
**404** — não encontrada

```bash
curl -s http://localhost:8080/api/v1/emotional/journal/$JOURNAL_ID \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

### PUT /emotional/journal/{id}

Substitui o conteúdo de uma entrada (PUT semântico — substituição completa). O novo conteúdo é cifrado novamente.

**Body**
```json
{
  "content": "Revisando minha entrada: na verdade o dia foi mais tranquilo do que pareceu inicialmente."
}
```

**200 OK** — `JournalEntryResult` atualizado  
**400** — conteúdo inválido  
**403** — entrada não pertence ao usuário  
**404** — não encontrada

```bash
curl -s -X PUT http://localhost:8080/api/v1/emotional/journal/$JOURNAL_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content":"Conteúdo revisado."}' | jq
```

---

### DELETE /emotional/journal/{id}

Remove uma entrada permanentemente. O ciphertext é descartado sem possibilidade de recuperação.

**204 No Content**  
**403** — entrada não pertence ao usuário  
**404** — não encontrada

```bash
curl -s -X DELETE http://localhost:8080/api/v1/emotional/journal/$JOURNAL_ID \
  -H "Authorization: Bearer $TOKEN" -w "%{http_code}"
```

---

## Tendência Emocional

### GET /emotional/trend?userId=&periodDays=

Retorna a tendência de humor/energia no período.

> ⚠️ **Aviso:** dado informativo — não constitui diagnóstico médico ou psicológico.

**Query params:**

| Param | Padrão | Descrição |
|-------|--------|-----------|
| `userId` | (próprio usuário) | UUID do Titular a ser consultado. Para consultar outro usuário, o Apoio precisa da permissão `EMOTIONAL_CHECKIN` concedida no vínculo. |
| `periodDays` | `30` | Janela de análise em dias (retroativa) |

**200 OK**
```json
{
  "dataPoints": [
    {
      "id": "checkin-uuid",
      "moodScore": 4,
      "energyLevel": 3,
      "moodLevel": "HIGH",
      "checkInDate": "2026-11-26",
      "checkInTime": "14:30:00",
      "notes": null
    }
  ],
  "averageMood": 3.75,
  "averageEnergy": 3.2,
  "periodDays": 30,
  "trend": "IMPROVING",
  "disclaimer": "Este dado é informativo e NÃO constitui diagnóstico clínico."
}
```

| Campo | Valores | Descrição |
|-------|---------|-----------|
| `trend` | `IMPROVING`, `STABLE`, `DECLINING` | Calculado comparando primeira e segunda metade do período |
| `dataPoints` | lista | Check-ins individuais do período (notas sempre `null` ao consultar outro usuário) |

**403** — Apoio sem permissão `EMOTIONAL_CHECKIN` para o Titular informado

```bash
# Próprio usuário
curl -s "http://localhost:8080/api/v1/emotional/trend?periodDays=30" \
  -H "Authorization: Bearer $TOKEN" | jq

# Apoio consultando Titular (exige permissão EMOTIONAL_CHECKIN no vínculo)
curl -s "http://localhost:8080/api/v1/emotional/trend?userId=$TITULAR_ID&periodDays=30" \
  -H "Authorization: Bearer $TOKEN_APOIO" | jq
```

---

## Notas de privacidade

- Os campos `notes` dos check-ins e o `content` do diário são armazenados como `BYTEA` cifrado (AES-256-GCM) na tabela `ng_sensitive.ng_mood_checkins` e `ng_sensitive.ng_journal_entries`.
- A aplicação decifra apenas ao retornar dados ao próprio usuário autenticado.
- O Apoio **nunca** acessa conteúdo bruto — apenas scores numéricos agregados na tendência.
- Ao consultar a tendência de outro usuário, o campo `notes` é sempre `null` nos dataPoints.

---

## Fluxo completo com curl

```bash
export TOKEN="<access_token>"

# 1. Registrar check-in do dia
CHECKIN=$(curl -s -X POST http://localhost:8080/api/v1/emotional/checkins \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"moodScore":4,"energyLevel":3,"notes":"Dia produtivo"}')
export CHECKIN_ID=$(echo $CHECKIN | jq -r .id)

# 2. Escrever no diário vinculando ao check-in
JOURNAL=$(curl -s -X POST http://localhost:8080/api/v1/emotional/journal \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"content\":\"Hoje consegui manter o foco.\",\"relatedCheckInId\":\"$CHECKIN_ID\"}")
export JOURNAL_ID=$(echo $JOURNAL | jq -r .id)

# 3. Listar check-ins dos últimos 7 dias
curl -s "http://localhost:8080/api/v1/emotional/checkins?periodDays=7" \
  -H "Authorization: Bearer $TOKEN" | jq 'length'

# 4. Listar entradas de diário
curl -s http://localhost:8080/api/v1/emotional/journal \
  -H "Authorization: Bearer $TOKEN" | jq 'length'

# 5. Editar entrada de diário
curl -s -X PUT http://localhost:8080/api/v1/emotional/journal/$JOURNAL_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content":"Revisado: foi um ótimo dia."}' | jq .updatedAt

# 6. Ver tendência do mês
curl -s "http://localhost:8080/api/v1/emotional/trend" \
  -H "Authorization: Bearer $TOKEN" | jq '{trend,averageMood,averageEnergy}'

# 7. Remover check-in
curl -s -X DELETE http://localhost:8080/api/v1/emotional/checkins/$CHECKIN_ID \
  -H "Authorization: Bearer $TOKEN" -w "\nHTTP %{http_code}"
```
