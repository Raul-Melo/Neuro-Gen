# Checkpoint 3 — Entregas e Evidências

Mapa de cada item do Checkpoint 3 para onde ele pode ser verificado no repositório.

| Item | Entrega | Onde verificar |
|------|---------|----------------|
| 4.8 | Tecnologia de backend definida | ADR-002 em [`docs/defContexto/05-adrs.md`](../defContexto/05-adrs.md) · seção "Stacks e Arquitetura" do [`README`](../../README.md) |
| 4.8 | Serviços REST implementados | Controllers em `src/main/java/**/api/` · Swagger UI em `/swagger-ui.html` (local ou na nuvem) |
| 4.8 | Uso de TDD | ADR-008 · ciclos no histórico (`git log --oneline --grep=TDD`) · `src/test/java` · aba *Actions* do GitHub |
| 4.8 | Implantação em nuvem | ADR-007 · [`docs/deploy/README.md`](../deploy/README.md) · `Dockerfile` · `render.yaml` · URL pública no README |
| 4.9 | Diagramas de classes das interações entre componentes | [`docs/diagramas/05-interacoes-componentes.md`](../diagramas/05-interacoes-componentes.md) (complementa `01-diagrama-classes.md`) |
| 4.9.2 | Atualização no LibreProject | Realizada pela equipe na plataforma LibreProject (fora deste repositório) |
| 4.9.3 | Commits semanais com a evolução do código | Histórico de commits e convenção descrita abaixo |

---

## 4.8 — Serviços REST

A API é organizada por módulo (bounded context) e documentada com OpenAPI, agrupada no Swagger UI:

| Grupo no Swagger | Base path | Principais operações |
|------------------|-----------|----------------------|
| 1-identity | `/api/v1/auth`, `/api/v1/users` | registro, login (JWT), refresh, logout, verificação de e-mail, reset de senha, 2FA |
| 2-link | `/api/v1/links` | convite Titular/Apoio, aceite, permissões por módulo, revogação |
| 3-agenda | `/api/v1/events`, `/api/v1/tasks` | eventos com recorrência e lembretes; tarefas, subtarefas e ciclo de vida |
| 4-focus | `/api/v1/focus` | sessões de foco: iniciar, pausar, retomar, encerrar, histórico |
| 5-emotional | `/api/v1/emotional` | check-in de humor/energia, diário (dados sensíveis cifrados), tendência |
| 6-vault | `/api/v1/vault` | cofre zero-knowledge com datasource isolado; negado a contas APOIO |

Erros seguem um formato único (`ErrorResponse`) com `code` estável para o cliente tratar, por exemplo
`VALIDATION_ERROR`, `INVALID_TASK_TRANSITION` e `RESOURCE_NOT_FOUND`.

## 4.8 — TDD

Cada comportamento novo deste checkpoint seguiu o ciclo **RED → GREEN (→ REFACTOR)**, com um commit por
etapa. A mensagem de cada commit registra o estado da suíte naquele momento:

| Ciclo | Comportamento especificado pelo teste | Testes |
|-------|----------------------------------------|--------|
| 1 | Máquina de estados da Tarefa (ex.: não concluir tarefa arquivada → 409) e validação de título | `TaskTest` |
| 2 | Evento cancelado não pode ser alterado (409) e validação de título; refactor extraiu `AgendaTitle` | `EventTest` |
| 3 | Filtro `?status=` inválido retorna 400 em vez de 500 e aceita minúsculas | `TaskUseCaseTest` |
| 4 | Chave AES-256 aceita Base64 (formato dos geradores de segredo da nuvem) além de hexadecimal | `AesGcmCipherTest` |
| 5 | Erros de requisição do cliente retornam 4xx em vez de 500 (JSON malformado, parâmetro ausente etc.) | `GlobalExceptionHandlerTest` |

Os ciclos 3 a 5 corrigiram defeitos reais encontrados ao preparar a implantação: sem o ciclo 4, por
exemplo, a aplicação não subiria na nuvem com a chave gerada pelo Render.

**Pirâmide de testes** (ADR-008): domínio com JUnit/AssertJ puro → casos de uso com Mockito → API com
MockMvc → integração com Testcontainers e PostgreSQL 16 real. O **CI** (GitHub Actions) executa tudo a
cada push e falha se o teste de integração for pulado.

## 4.8 — Implantação em nuvem

`git push` → **CI** verde → **Render** constrói a imagem Docker → serviço Spring Boot (perfil `prod`)
conectado ao PostgreSQL 16 gerenciado. Detalhes, variáveis e roteiro de demonstração em
[`docs/deploy/README.md`](../deploy/README.md); justificativa e limitações no ADR-007.

## 4.9 — Diagramas de interação

[`docs/diagramas/05-interacoes-componentes.md`](../diagramas/05-interacoes-componentes.md) traz 5 diagramas
de classes com os nomes reais do código: requisição REST pelas camadas, autenticação JWT, integração entre
módulos (evento `LinkRevoked` e porta `SupportAccessPolicy`), isolamento do Cofre e tratamento de erros.

## 4.9.3 — Convenção de commits e cadência

- **Cadência:** ao menos um push por semana em cada branch de trabalho ativa, e cada integrante versiona
  a parte que desenvolveu.
- **Fluxo de branches:** `feature/*` → Pull Request para `develop` → Pull Request para `main`. O merge só
  acontece com o check de CI verde; a `main` é o que vai para a nuvem.
- **Mensagens** no padrão *Conventional Commits*, em português:

| Tipo | Uso | Exemplo |
|------|-----|---------|
| `test` | testes; com `[TDD-RED]` quando especificam comportamento ainda não implementado | `test(agenda): especifica ... [TDD-RED]` |
| `feat` / `fix` | funcionalidade / correção; com `[TDD-GREEN]` quando fazem testes passarem | `fix(agenda): rejeita filtro ... [TDD-GREEN]` |
| `refactor` | melhoria interna sem mudar comportamento | `refactor(agenda): extrai ... [TDD-REFACTOR]` |
| `docs` · `ci` · `build` · `deploy` · `chore` | documentação, pipeline, build, infraestrutura, manutenção | `ci: adiciona GitHub Actions ...` |

---

## Roteiro sugerido para a apresentação (~12 min)

| Tempo | Conteúdo | Material |
|-------|----------|----------|
| 2 min | Stack e arquitetura: monólito modular hexagonal, por que Java/Spring/PostgreSQL | README, ADR-001/002 |
| 3 min | Diagramas de interação: camadas (1), integração entre módulos (3) e Cofre (4) | `05-interacoes-componentes.md` no GitHub |
| 3 min | TDD: um ciclo RED → GREEN no histórico, o teste correspondente e o CI verde | `git log --grep=TDD`, `TaskTest`, aba *Actions* |
| 3 min | Demonstração ao vivo na nuvem: Swagger, login, criar e concluir tarefa, regra 409 | Roteiro em `docs/deploy/README.md` |
| 1 min | Limitações conhecidas e próximos passos | ADR-007 (consequências) |

**Checklist do dia:**

- [ ] CI verde na `main` (badge do README).
- [ ] URL da nuvem aberta ~5 min antes (o plano gratuito hiberna).
- [ ] Usuário de demonstração criado e token à mão, ou criar ao vivo.
- [ ] Banco gratuito do Render criado há menos de 30 dias.
