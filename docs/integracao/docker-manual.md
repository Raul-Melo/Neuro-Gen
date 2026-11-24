# Manual Docker — Neuro-Gen

---

## Estrutura dos arquivos Docker

| Arquivo | Finalidade |
|---|---|
| `compose.yaml` | **Dev padrão** — sobe apenas Postgres 16 + pgAdmin. Use com `.\mvnw spring-boot:run`. |
| `docker-compose.full.yml` | **Validação completa** — sobe Postgres + pgAdmin + imagem da aplicação compilada. |
| `Dockerfile` | Build multi-estágio (Maven compile → JRE Alpine). Usado pelo `docker-compose.full.yml`. |
| `.env.example` | Template de variáveis de ambiente sensíveis. Copie para `.env` e preencha. |

---

## Contexto: portas e conflito com PostgreSQL local

O `compose.yaml` mapeia o PostgreSQL do container para a **porta 5433** da máquina host (não 5432).

**Por quê?** No Windows, a instalação padrão do PostgreSQL cria o serviço `postgresql-x64-*` que já ocupa a porta 5432. Usar 5433 evita o conflito sem precisar desligar o serviço local.

| Onde | Host | Porta |
|---|---|---|
| PostgreSQL local (Windows) | localhost | 5432 |
| PostgreSQL Docker (Neuro-Gen) | localhost | **5433** |

O `application.yml` já está configurado para `localhost:5433`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/neurogen
vault:
  datasource:
    url: jdbc:postgresql://localhost:5433/neurogen
```

> Se você **não** tiver PostgreSQL local instalado, pode alterar para `5432:5432` e `localhost:5432` sem problema.

---

## Modo 1 — Desenvolvimento (banco no Docker, app local)

Este é o modo mais rápido para desenvolver: o banco roda em Docker com healthcheck e a aplicação roda localmente com hot-reload.

```powershell
# Subir banco + pgAdmin
docker compose up -d

# Verificar se o banco está saudável
docker compose ps

# Iniciar aplicação (Flyway roda as migrations na inicialização)
.\mvnw spring-boot:run
```

Para parar:

```powershell
docker compose down
```

Para parar e apagar os dados (volume):

```powershell
docker compose down -v
```

---

## Modo 2 — Completo (tudo no Docker)

```powershell
# 1. Preparar variáveis
cp .env.example .env
# Edite .env e defina pelo menos SENSITIVE_DATA_KEY e DB_PASSWORD

# 2. Build e subida
docker compose -f docker-compose.full.yml --env-file .env up --build -d

# 3. Acompanhar logs
docker compose -f docker-compose.full.yml logs -f app

# 4. Verificar saúde dos containers
docker compose -f docker-compose.full.yml ps
```

Aguarde o container `neurogen-app` aparecer como `healthy` (pode levar ~60s no primeiro build).

Para parar:

```powershell
docker compose -f docker-compose.full.yml down
```

---

## Serviços e portas

| Container | Imagem | Porta (host) | URL |
|---|---|---|---|
| `neurogen-postgres` | `postgres:16-alpine` | **127.0.0.1:5433** | — |
| `neurogen-pgadmin` | `dpage/pgadmin4` | **127.0.0.1:5050** | http://localhost:5050 |
| `neurogen-app` | Build local | 8080 | http://localhost:8080 |

> Ambas as portas estão vinculadas ao **loopback** (`127.0.0.1`), não a `0.0.0.0`. Isso impede acesso de outras máquinas na rede local — adequado para ambiente de desenvolvimento.

---

## Segurança do banco em desenvolvimento

O `compose.yaml` usa autenticação **`scram-sha-256`** (padrão do PostgreSQL) com as credenciais:

```
Usuário:  neurogen
Senha:    secret
Banco:    neurogen
```

A porta é vinculada a `127.0.0.1:5433`, o que garante que apenas conexões da máquina local chegam ao container. Essa combinação é adequada para desenvolvimento local.

> **Histórico:** versões anteriores usavam `POSTGRES_HOST_AUTH_METHOD: trust` com a porta exposta em `0.0.0.0:5433`. Isso foi corrigido em novembro/2026 para usar autenticação por senha + binding de loopback.

> **Em produção:** use uma instância gerenciada (RDS, Cloud SQL, Supabase) com SSL obrigatório, senha forte via secrets manager e pg_hba.conf restritivo.

---

## Conectar ao banco com psql

```powershell
# De dentro do container (sem precisar de psql instalado localmente)
docker exec -it neurogen-postgres psql -U neurogen -d neurogen

# Da sua máquina (se psql instalado) — usar porta 5433
psql -h localhost -p 5433 -U neurogen -d neurogen
# Senha: secret
```

---

## pgAdmin — adicionar servidor

1. Abra http://localhost:5050
2. Login: `admin@admin.com` / `admin` (ou valor de `PGADMIN_PASSWORD` no `.env`)
3. Clique com o botão direito em **Servers → Register → Server**
4. **General → Name:** `Neuro-Gen Dev`
5. **Connection:**
   - Host: `postgres` (nome do serviço Docker — funciona dentro da rede Docker)
   - Port: `5432`
   - Maintenance DB: `neurogen`
   - Username: `neurogen`
   - Password: `secret`

> Se conectar da máquina host (fora do Docker), use `localhost` como host e porta `5433`.

---

## Verificar as migrations Flyway

```sql
SELECT version, description, installed_on, success
FROM ng_core.flyway_schema_history
ORDER BY installed_rank;
```

Schemas criados automaticamente pelo Flyway:

| Schema | Propósito | Migration |
|---|---|---|
| `ng_identity` | Usuários, sessões, TOTP, tokens de verificação | V1 |
| `ng_core` | Vínculos, agenda, foco, notificações | V2 |
| `ng_sensitive` | Dados de saúde cifrados (LGPD) | V3 |
| `ng_audit` | Auditoria imutável de banco e logs de sistema | V5 |
| `ng_vault` | Cofre zero-knowledge (datasource isolado) | V1-vault |

A tabela `ng_core.flyway_schema_history` guarda o histórico do core. O vault tem sua própria tabela de histórico no schema `ng_vault`.

---

## Recriar banco do zero

```powershell
# Para tudo e apaga volumes
docker compose down -v

# Sobe novamente (Flyway cria tudo do zero)
docker compose up -d
.\mvnw spring-boot:run
```

---

## Executar os testes com banco real (Testcontainers)

O `NeuroGenIntegrationTest` usa Testcontainers para subir um PostgreSQL 16 isolado durante os testes. **Requer Docker Desktop rodando.** Não interfere com o container de desenvolvimento.

```powershell
# Rodar apenas o teste de integração
.\mvnw test -Dtest=NeuroGenIntegrationTest

# Rodar toda a suite (90 testes)
.\mvnw test
```

O Testcontainers:
- Sobe um `postgres:16` temporário na primeira vez que o teste roda
- Aplica todas as migrations Flyway (core + vault)
- Valida que a regra de segurança RF-08.5 está em vigor (conta APOIO → 403 no Vault, auditado)
- Destrói o container ao final — não deixa rastro

---

## Variáveis do compose.yaml

As variáveis de ambiente da aplicação só são necessárias no `docker-compose.full.yml` (modo app no Docker). No modo dev, configure em `src/main/resources/application.yml`.

| Variável | Valor padrão no compose | Descrição |
|---|---|---|
| `DB_HOST` | `postgres` | Nome do serviço PostgreSQL |
| `DB_PORT` | `5432` | Porta interna Docker |
| `DB_NAME` | `neurogen` | Banco |
| `DB_USERNAME` | `neurogen` | Usuário |
| `DB_PASSWORD` | _(via .env)_ | **Sem default** — falha intencionalmente se não definido |
| `SENSITIVE_DATA_KEY` | _(via .env)_ | **Obrigatória** — 64 chars hex |
| `SPRING_PROFILES_ACTIVE` | `prod` | Perfil Spring |

---

## Healthchecks

Ambos os composes definem healthchecks:

- **postgres:** `pg_isready -U neurogen -d neurogen` — intervalo 10s, 5 tentativas, start_period 20s
- **app (full only):** `wget http://localhost:8080/actuator/health/liveness` — intervalo 30s, 5 tentativas, start_period 60s

A aplicação só sobe depois que o `postgres` fica `healthy` (`depends_on: condition: service_healthy`).

---

## Monitoramento básico

```powershell
# Saúde da aplicação
curl http://localhost:8080/actuator/health

# Saúde detalhada (liveness e readiness)
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness

# Logs do banco (últimas 100 linhas)
docker logs --tail 100 neurogen-postgres

# Logs da app (últimas 100 linhas + follow)
docker logs -f --tail 100 neurogen-app

# Status de todos os containers
docker compose ps
```

---

## Geração de SENSITIVE_DATA_KEY

```powershell
# Windows PowerShell
-join ((1..32) | ForEach-Object { '{0:x2}' -f (Get-Random -Max 256) })
```

```bash
# Linux / macOS
openssl rand -hex 32
```

O resultado deve ter exatamente **64 caracteres hexadecimais**. Exemplo:
```
a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2
```

Coloque no arquivo `.env`:
```
SENSITIVE_DATA_KEY=a1b2c3d4...
DB_PASSWORD=suaSenhaSegura
PGADMIN_PASSWORD=suaSenhaAdmin
```

---

## Integração com frontend (CORS)

O backend permite requisições de frontends locais nas portas 3000 e 5173. Não é necessário proxy no frontend — as requisições podem ir diretamente para `http://localhost:8080`.

Se o frontend usar proxy configurado (ex.: `vite.config.ts` ou `next.config.js`), o CORS não é relevante para as chamadas proxied — mas os cookies `SameSite` e os headers `Authorization` continuam funcionando normalmente.

> **Registre um usuário antes de fazer login:** o banco inicia vazio. Use `POST /api/v1/auth/register` para criar a primeira conta.

---

## Troubleshooting

### `password authentication failed for user "neurogen"`

Causa mais comum: o `application.yml` está apontando para `localhost:5432` (PostgreSQL local), não para o container em `localhost:5433`.

Verifique:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/neurogen  # deve ser 5433
```

### `DockerProcessStartException` ao iniciar com `spring-boot:run`

O Spring Boot DevTools tenta gerenciar o `compose.yaml` automaticamente, mas o `docker.exe` não está no PATH do processo Java. Solução já aplicada no `application.yml`:

```yaml
spring:
  docker:
    compose:
      enabled: false
```

### `Port 8080 was already in use`

A aplicação já está rodando (processo Java anterior). Encerre-a antes de iniciar novamente:

```powershell
Get-Process -Name "java" | Stop-Process -Force
```

### Container postgres não fica `healthy`

```powershell
# Ver logs do container
docker logs neurogen-postgres

# Verificar pg_isready manualmente
docker exec neurogen-postgres pg_isready -U neurogen -d neurogen
```

### `FATAL: database "neurogen" does not exist`

O volume foi criado com um banco diferente. Recrie:

```powershell
docker compose down -v
docker compose up -d
```
