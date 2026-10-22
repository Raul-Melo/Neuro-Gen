# Neuro-Gen — Requisitos Funcionais

**Versão:** 0.2 (aprofundado)
**Referência:** `01-visao-produto.md`
**Muda em relação à v0.1:** granularidade de subrequisitos, critérios de aceitação (Gherkin) para todo item Must, entidades de dados por épico, regras de negócio e edge cases explícitos, 3 épicos novos (Onboarding, Integrações Externas, Administração de Notificações).

Numeração: `RF-[ÉPICO].[SEQ]`. Prioridade em MoSCoW.

---

## EPIC-01 — Contas, Autenticação e Vínculo Titular/Apoio

**Entidades:** `User (papel: titular|apoio)`, `Link (titular_id, apoio_id, status, permissoes[])`, `ConsentRecord`, `Session`

| ID | Requisito | Prioridade |
|---|---|---|
| RF-01.1 | Cadastro de conta Titular via e-mail/senha, Google, Apple ID | Must |
| RF-01.2 | Cadastro de conta Apoio via convite (link com token expirável, TTL 72h) | Must |
| RF-01.3 | Titular define, por módulo, visibilidade granular por Apoio vinculado (não é global — cada vínculo tem sua própria matriz de permissão) | Must |
| RF-01.4 | Titular revoga vínculo a qualquer momento, com efeito imediato (< 5s de propagação) | Must |
| RF-01.5 | Fluxo de consentimento parental obrigatório quando Titular declara idade < 18 anos no cadastro | Must |
| RF-01.6 | Verificação de e-mail obrigatória antes de liberar funcionalidades além de leitura | Must |
| RF-01.7 | Recuperação de senha via e-mail com expiração de token (15 min) | Must |
| RF-01.8 | Apoio pode enviar lembrete/incentivo pontual ao Titular, sujeito a permissão "Enviar Lembretes" | Should |
| RF-01.9 | 2FA opcional (TOTP) para conta Titular; obrigatório para acesso ao Cofre (ver EPIC-08) | Should |
| RF-01.10 | Terapeuta/coach com múltiplos vínculos simultâneos, visão consolidada por Titular (sem misturar dados entre Titulares) | Could |
| RF-01.11 | Titular visualiza log de acessos de cada Apoio (quando acessou, o que visualizou) | Should |
| RF-01.12 | Bloqueio automático de novo vínculo se Titular menor não tiver consentimento parental ativo | Must |

**Regra de negócio crítica:** a ausência de permissão explícita = negada por padrão (opt-in, nunca opt-out). Nenhum módulo é visível a um Apoio recém-vinculado até o Titular conceder acesso.

**Critério de aceitação — RF-01.3:**
```
Dado que o Titular possui dois vínculos de Apoio (Mãe e Terapeuta)
Quando ele concede "Diário Emocional" apenas para Terapeuta
Então a Mãe não visualiza o módulo Diário Emocional em nenhuma tela,
E a Terapeuta visualiza normalmente,
E a alteração é refletida sem necessidade de logout/login do Apoio.
```

**Critério de aceitação — RF-01.4:**
```
Dado que um vínculo de Apoio está ativo
Quando o Titular revoga o vínculo
Então a sessão ativa do Apoio perde acesso aos dados do Titular em até 5 segundos,
E o Apoio recebe notificação de que o vínculo foi encerrado,
E o histórico de acesso permanece auditável para o Titular.
```

**Edge cases a tratar:**
- Titular menor que completa 18 anos: fluxo de transição de consentimento parental → autonomia plena.
- Convite de Apoio expira antes do aceite → novo link deve ser gerado, o antigo invalidado.
- Apoio removido enquanto está com sessão ativa em outro dispositivo → forçar refresh de permissões, não apenas no login.

---

## EPIC-02 — Agenda e Planner Personalizável

**Entidades:** `Event`, `Task`, `RecurrenceRule`, `Category/Tag`, `ViewPreference`, `RoutineTemplate`

| ID | Requisito | Prioridade |
|---|---|---|
| RF-02.1 | CRUD de eventos e tarefas com data, hora início/fim, recorrência (diária, semanal, mensal, customizada via regra RRULE) | Must |
| RF-02.2 | Visão calendário: dia, semana, mês | Must |
| RF-02.3 | Visão lista, agrupável por data, categoria ou prioridade | Must |
| RF-02.4 | Visão kanban com colunas customizáveis por status (ex: A fazer, Em andamento, Feito) | Must |
| RF-02.5 | Personalização visual: cor e ícone por categoria/tag, definidos pelo usuário | Must |
| RF-02.6 | Diferenciação visual entre evento (compromisso fixo) e tarefa (flexível/sem hora fixa) | Must |
| RF-02.7 | Quebra assistida de tarefa grande em subtarefas (sugestão automática configurável, não obrigatória) | Should |
| RF-02.8 | Templates de rotina reutilizáveis (criar uma vez, aplicar a qualquer dia/período) | Should |
| RF-02.9 | Time-blocking: alocar blocos de tempo na agenda vinculados a uma tarefa | Should |
| RF-02.10 | Modo "baixa densidade": oculta itens além do limite configurável por tela, com expansão sob demanda | Could |
| RF-02.11 | Duplicar evento/tarefa mantendo configurações (lembretes, tags) | Should |
| RF-02.12 | Arrastar e soltar (drag-and-drop) para reagendar em qualquer visão | Should |

**Critério de aceitação — RF-02.4:**
```
Dado que o Titular está na visão kanban
Quando ele arrasta uma tarefa da coluna "A fazer" para "Feito"
Então o status da tarefa é atualizado no backend,
E a tarefa reflete a mudança em todas as outras visões (calendário, lista) sem necessidade de reload manual.
```

**Regra de negócio:** recorrência editada pode aplicar a "somente esta ocorrência", "esta e futuras" ou "todas" — obrigatório perguntar ao usuário ao editar item recorrente (evita ambiguidade e erro de edição em massa).

---

## EPIC-03 — Lembretes e Notificações Inteligentes

**Entidades:** `Reminder`, `NotificationChannel`, `NotificationLog`, `NudgePattern`

| ID | Requisito | Prioridade |
|---|---|---|
| RF-03.1 | Múltiplos lembretes por evento/tarefa (ex: 1 dia antes, 1h antes, 10min antes), configuráveis livremente | Must |
| RF-03.2 | Lembrete persistente ("nudge"): repete a cada N minutos até confirmação de leitura, limitado a M repetições configuráveis | Must |
| RF-03.3 | Canal fallback: se push não confirmado em X minutos, envia e-mail (ou SMS, se habilitado) | Should |
| RF-03.4 | Confirmação de leitura registra timestamp e origem (qual dispositivo) | Must |
| RF-03.5 | Lembretes adaptativos: sistema sugere horário adicional de lembrete com base em histórico de itens perdidos | Could |
| RF-03.6 | Apoio dispara lembrete manual pontual, sujeito a permissão ativa | Should |
| RF-03.7 | Central de notificações in-app com histórico dos últimos 30 dias | Should |
| RF-03.8 | Usuário configura "janela de silêncio" (ex: não notificar entre 22h–7h, exceto itens marcados como críticos) | Must |

**Critério de aceitação — RF-03.2:**
```
Dado que uma tarefa tem lembrete configurado como "persistente, repetir a cada 5 min, até 3 vezes"
Quando a notificação é enviada e o usuário não interage em 5 minutos
Então uma nova notificação é enviada,
E esse ciclo se repete até o limite de 3 vezes ou até confirmação,
E, após o limite, o item é marcado como "lembrete não confirmado" visível no dashboard.
```

---

## EPIC-04 — Foco e Produtividade

**Entidades:** `FocusSession`, `FocusSettings`, `ScreenTimeAlert`

| ID | Requisito | Prioridade |
|---|---|---|
| RF-04.1 | Timer de foco configurável (duração de trabalho e pausa customizáveis, preset Pomodoro 25/5 como padrão) | Must |
| RF-04.2 | Modo foco silencia notificações não críticas durante a sessão ativa | Must |
| RF-04.3 | Vínculo opcional de sessão de foco a uma tarefa específica | Must |
| RF-04.4 | Alerta de tempo de tela contínuo (ex: após 90 min sem pausa registrada, sugere parar) | Should |
| RF-04.5 | Histórico de sessões (duração, tarefa vinculada, data) navegável por período | Should |
| RF-04.6 | Sugestão de micro-pausa ativa com conteúdo (alongamento, respiração) entre sessões | Could |
| RF-04.7 | Métrica de "tempo de foco efetivo" por dia/semana exibida ao usuário | Should |
| RF-04.8 | Permitir pausar/retomar sessão sem perder o tempo já contabilizado | Must |

**Critério de aceitação — RF-04.2:**
```
Dado que o usuário inicia uma sessão de foco
Quando uma notificação não crítica chega durante a sessão
Então ela é retida e entregue apenas ao final da sessão (ou descartada, conforme configuração do usuário),
E notificações marcadas como críticas (ex: lembrete de medicação) continuam sendo entregues normalmente.
```

---

## EPIC-05 — Regulação Emocional

**Entidades:** `MoodCheckIn`, `JournalEntry`, `EmotionalTrend`, `OverloadAlert`

| ID | Requisito | Prioridade |
|---|---|---|
| RF-05.1 | Check-in de humor/energia em escala visual (ex: 1–5, com ícones), preenchimento em < 10s | Must |
| RF-05.2 | Check-in pode ser feito múltiplas vezes ao dia ou 1x/dia, configurável | Must |
| RF-05.3 | Diário emocional textual opcional, associado (ou não) ao check-in do dia | Should |
| RF-05.4 | Dashboard de tendência (linha do tempo de humor/energia, 7/30/90 dias) | Should |
| RF-05.5 | Correlação visual entre carga de agenda (nº de eventos/tarefas) e padrão emocional do mesmo período | Could |
| RF-05.6 | Alerta ao Apoio autorizado em caso de padrão sustentado de baixa pontuação (ex: 5 dias consecutivos abaixo do limiar), somente com consentimento explícito e configurável pelo Titular | Could |
| RF-05.7 | Conteúdo de apoio contextual (ex: técnica de respiração) sugerido após check-in de baixa pontuação — não prescritivo, apenas informativo | Should |
| RF-05.8 | Exportação do histórico emocional em PDF/CSV para uso com terapeuta | Should |

**Regra de negócio:** o produto não realiza diagnóstico nem interpretação clínica — qualquer insight é apresentado como "padrão observado", nunca como recomendação médica. Texto de disclaimer obrigatório em telas de insight (ver RNF-PRIV).

---

## EPIC-06 — Organização

**Entidades:** `TaskList`, `Subtask`, `Category`, `Attachment`

| ID | Requisito | Prioridade |
|---|---|---|
| RF-06.1 | Listas de tarefas sem data (backlog pessoal), múltiplas listas nomeáveis | Must |
| RF-06.2 | Subtarefas com checklist e progresso percentual visível na tarefa-mãe | Must |
| RF-06.3 | Categorização por contexto (predefinidas + customizáveis pelo usuário) | Must |
| RF-06.4 | Busca global (texto) com filtro por tag, categoria, data, status | Should |
| RF-06.5 | Anexar arquivo/nota a uma tarefa (limite de tamanho configurável, ex: 10MB) | Could |
| RF-06.6 | Reordenação manual de itens dentro de uma lista (drag) | Should |
| RF-06.7 | Arquivamento de tarefas concluídas (não exclui, apenas remove da visão ativa) | Should |

---

## EPIC-07 — Otimização e Performance Pessoal

**Entidades:** `ProductivityMetric`, `WeeklyReport`

| ID | Requisito | Prioridade |
|---|---|---|
| RF-07.1 | Dashboard de tarefas concluídas vs. planejadas, por período configurável | Should |
| RF-07.2 | Heatmap de horários de maior produtividade (baseado em conclusão de tarefas + sessões de foco) | Should |
| RF-07.3 | Relatório semanal automático (in-app + opcional por e-mail), compartilhável com Apoio se permitido | Could |
| RF-07.4 | Métrica de taxa de conclusão por categoria/tag (identifica áreas de maior dificuldade) | Could |

---

## EPIC-08 — Cofre de Senhas e Segredos

**Entidades:** `VaultEntry (payload criptografado)`, `VaultAuthLog`, `MasterKeyMeta`

| ID | Requisito | Prioridade |
|---|---|---|
| RF-08.1 | CRUD de entradas (login, senha, URL, notas, campos customizados) | Must |
| RF-08.2 | Criptografia client-side (zero-knowledge): payload cifrado antes de sair do dispositivo, servidor nunca vê texto claro | Must |
| RF-08.3 | Senha mestra distinta da senha de login do app, obrigatória para abrir o Cofre | Must |
| RF-08.4 | Biometria (Face ID/Touch ID/impressão digital Android) como atalho pós-configuração da senha mestra | Should |
| RF-08.5 | Cofre nunca exposto a nenhuma conta de Apoio, independentemente de permissões configuradas em outros módulos — restrição hard-coded, não configurável | Must |
| RF-08.6 | Timeout de sessão do Cofre (auto-lock após X minutos de inatividade, configurável, padrão 5 min) | Must |
| RF-08.7 | Gerador de senha segura (comprimento/complexidade configurável) | Should |
| RF-08.8 | Autofill em navegador (extensão) e app mobile | Could |
| RF-08.9 | Exportação criptografada para backup local (arquivo protegido por senha) | Could |
| RF-08.10 | Alerta de senha fraca ou reutilizada entre entradas do Cofre | Could |
| RF-08.11 | Processo de recuperação de senha mestra: apenas via chave de recuperação gerada no setup (exibida uma única vez) — sem "esqueci minha senha" tradicional, pois isso quebraria zero-knowledge | Must |

**Critério de aceitação — RF-08.5:**
```
Dado que um Apoio tem todas as permissões concedidas em todos os outros módulos
Quando esse Apoio acessa a conta do Titular
Então o módulo Cofre não é listado, não é acessível via URL direta, e nenhuma API retorna dados do Cofre para esse Apoio,
E qualquer tentativa é registrada em log de segurança e bloqueada no nível de backend (não apenas ocultada na UI).
```

**Critério de aceitação — RF-08.11:**
```
Dado que o Titular configurou o Cofre pela primeira vez
Quando o setup é concluído
Então uma chave de recuperação única é exibida uma única vez, com instrução explícita de salvá-la offline,
E o sistema não permite reset de senha mestra sem essa chave,
E, sem a chave, os dados do Cofre são permanentemente irrecuperáveis (trade-off aceito e comunicado ao usuário).
```

---

## EPIC-09 — Disponibilidade Multiplataforma

| ID | Requisito | Prioridade |
|---|---|---|
| RF-09.1 | Web app responsivo com paridade funcional total nos itens Must de todos os épicos | Must |
| RF-09.2 | App Android nativo/híbrido com paridade funcional total nos itens Must | Must |
| RF-09.3 | App iOS nativo/híbrido com paridade funcional total nos itens Must | Must |
| RF-09.4 | Sincronização em tempo real entre sessões/dispositivos do mesmo usuário | Must |
| RF-09.5 | Modo offline: visualizar agenda/tarefas e criar/editar itens sem conexão, com fila de sincronização ao reconectar | Should |
| RF-09.6 | Resolução de conflito de sincronização determinística (last-write-wins com timestamp de servidor + notificação ao usuário se houve conflito real) | Should |
| RF-09.7 | Notificações push nativas (FCM para Android, APNs para iOS) | Must |
| RF-09.8 | Deep linking: notificação abre diretamente o item relacionado no app | Should |

---

## EPIC-10 — Onboarding e Perfil de Neurodivergência (novo)

**Objetivo estratégico:** personalizar a experiência desde o primeiro uso conforme o perfil declarado, sem rotular ou limitar o usuário.

**Entidades:** `UserProfile (perfil_declarado[], preferencias_sensoriais)`, `OnboardingProgress`

| ID | Requisito | Prioridade |
|---|---|---|
| RF-10.1 | Onboarding pergunta perfil de neurodivergência (múltipla escolha + opção "prefiro não informar"), usado apenas para sugerir configurações iniciais, nunca obrigatório | Must |
| RF-10.2 | Sugestão automática de configurações iniciais com base no perfil (ex: TDAH → lembretes persistentes ativados por padrão; TEA → modo baixa densidade + estímulo reduzido ativado por padrão) | Should |
| RF-10.3 | Onboarding curto (máx. 5 telas), com opção de pular e configurar depois | Must |
| RF-10.4 | Usuário pode alterar perfil declarado e reconfigurar sugestões a qualquer momento nas configurações | Must |
| RF-10.5 | Tutorial contextual (tooltips) na primeira vez que cada módulo principal é aberto, dispensável permanentemente | Should |

**Regra de negócio:** o perfil declarado nunca é usado para restringir funcionalidades — apenas para sugerir defaults. Usuário sempre com controle total de override.

---

## EPIC-11 — Integrações Externas (novo)

**Entidades:** `ExternalCalendarLink`, `SyncLog`

| ID | Requisito | Prioridade |
|---|---|---|
| RF-11.1 | Importar/sincronizar com Google Calendar (via API oficial) | Should |
| RF-11.2 | Importar/sincronizar com Apple Calendar (via CalDAV) | Could |
| RF-11.3 | Importar/sincronizar com Outlook/Microsoft 365 | Could |
| RF-11.4 | Sincronização bidirecional configurável (somente importar vs. importar e exportar) | Could |
| RF-11.5 | Usuário escolhe quais calendários externos ficam visíveis no Neuro-Gen (evita poluição de agenda) | Should |

---

## EPIC-12 — Administração e Central de Notificações do Sistema (novo)

**Objetivo estratégico:** operação e observabilidade do produto (não é feature de usuário final, mas necessária ao escopo funcional do sistema).

| ID | Requisito | Prioridade |
|---|---|---|
| RF-12.1 | Painel administrativo interno para suporte visualizar status de conta (sem acesso a dados sensíveis de conteúdo — apenas metadados operacionais) | Should |
| RF-12.2 | Sistema de fila para envio de notificações em massa sem degradar performance de API principal | Must |
| RF-12.3 | Health check e status page pública (uptime) | Should |
| RF-12.4 | Ferramenta interna de auditoria de acessos ao Cofre (equipe de segurança, acesso restrito e logado) | Should |

---

## Escopo de MVP (revisão)

MVP = todos os itens **Must** dos EPIC-01 a EPIC-10 e EPIC-12 (RF-12.2 é infraestrutura crítica de RF-03; demais itens de EPIC-12 podem esperar). **EPIC-11 (Integrações Externas)** não entra no MVP — é valor agregado pós-lançamento, não parte do loop core (organizar → lembrar → focar → regular → proteger).

Dependência crítica de sequenciamento: RF-01 (vínculo/permissões) → RF-08 (Cofre) → demais módulos. Cofre não pode ir a produção antes do motor de permissões estar validado e testado (ver RNF de segurança).
