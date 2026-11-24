# API — Emotional (Regulação Emocional)

Base URL: `/api/v1/emotional`

Todos os endpoints exigem `Authorization: Bearer <accessToken>`.

> **Dado sensível (LGPD):** os campos `notes` e o conteúdo do diário são cifrados em repouso (AES-256-GCM) na tabela `ng_sensitive`. O Apoio só pode ver a **tendência** do Titular se a permissão `EMOTIONAL_CHECKIN` estiver concedida no vínculo.

---

## POST /emotional/checkins

Registra um check-in de humor e energia do dia atual.

**Regra:** apenas **um check-in por dia** por usuário.

**Body**
```json
{
  "moodScore": 7,
  "energyLevel": 6,
  "notes": "Dia produtivo, foco melhorou no período da tarde."
}
```

- `moodScore`: 1–10 (1 = muito ruim, 10 = excelente)
- `energyLevel`: 1–10
- `notes`: opcional, texto livre (cifrado em repouso)

**201 Created**
```json
{
  "id": "checkin-uuid",
  "moodScore": 7,
  "energyLevel": 6,
  "checkInDate": "2025-01-19",
  "checkInTime": "14:30:00",
  "createdAt": "2025-01-19T14:30:00Z"
}
```

**curl**
```bash
curl -s -X POST http://localhost:8080/api/v1/emotional/checkins \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"moodScore":7,"energyLevel":6}' | jq
```

---

## POST /emotional/journal

Escreve uma entrada de diário emocional. O conteúdo é cifrado pelo servidor antes de persistir.

**Body**
```json
{
  "content": "Hoje consegui manter o foco por mais de 2 horas sem interrupções...",
  "relatedCheckInId": "checkin-uuid-opcional"
}
```

**201 Created**
```json
{ "message": "Entrada registrada: journal-entry-uuid" }
```

---

## GET /emotional/trend?userId=&periodDays=

Retorna a tendência de humor/energia no período.

**Query params:**
- `userId` (opcional): UUID do Titular a ser consultado. Se omitido, retorna os dados do próprio usuário. Para consultar outro usuário, o Apoio precisa da permissão `EMOTIONAL_CHECKIN` concedida.
- `periodDays` (padrão: `30`): janela de análise em dias.

**200 OK**
```json
{
  "ownerId": "user-uuid",
  "periodDays": 30,
  "totalCheckIns": 22,
  "avgMoodScore": 6.8,
  "avgEnergyLevel": 6.1,
  "moodTrend": "STABLE",
  "energyTrend": "IMPROVING",
  "disclaimer": "Este dado é informativo e NÃO constitui diagnóstico clínico."
}
```

`moodTrend` / `energyTrend` aceita: `IMPROVING`, `STABLE`, `DECLINING`, `INSUFFICIENT_DATA`.

**curl**
```bash
curl -s "http://localhost:8080/api/v1/emotional/trend?periodDays=30" \
  -H "Authorization: Bearer $TOKEN" | jq

# Apoio consultando Titular (exige permissão EMOTIONAL_CHECKIN no vínculo)
curl -s "http://localhost:8080/api/v1/emotional/trend?userId=$TITULAR_ID&periodDays=30" \
  -H "Authorization: Bearer $TOKEN_APOIO" | jq
```

---

## Notas de privacidade

- As notas dos check-ins (`notes_enc`) e o conteúdo do diário (`content_enc`) são armazenados como `BYTEA` cifrado. A aplicação decifra somente ao retornar dados ao próprio Titular.
- O Apoio **nunca** acessa o conteúdo bruto — apenas a tendência agregada (scores e médias).
- A exclusão de conta apaga fisicamente os registros `ng_sensitive` (direito ao esquecimento, LGPD).
