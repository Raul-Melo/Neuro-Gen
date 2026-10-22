# Diagramas de Casos de Uso — Neuro-Gen

> **Atores do sistema:**
> - **Titular** — usuário neurodivergente, dono dos dados. Controla permissões e todos os módulos.
> - **Apoio** — responsável/terapeuta vinculado. Acessa apenas o que o Titular liberar (opt-in).
> - **Sistema** — processos automáticos (scheduler, fila de notificações, workers).
> - **Admin** — equipe operacional interna (acesso apenas a metadados, nunca a conteúdo sensível).
>
> **Regras transversais:**
> - Ausência de permissão explícita = acesso negado (opt-in — RF-01.3)
> - Cofre (Vault) NUNCA acessível por Apoio, independente de permissões (RF-08.5)
> - Toda ação sobre dado sensível exige verificação de permissão via `ng_link_permissions`

---

## UC-01: Autenticação e Gestão de Conta

```mermaid
flowchart LR
    %% Atores
    Titular(["👤 Titular"])
    Apoio(["👥 Apoio"])
    Sistema(["⚙️ Sistema"])

    %% Casos de uso — identidade como elipses (subgraph simula fronteira do sistema)
    subgraph identity ["🔐 Sistema: Identity (IAM)"]
        UC01_1(["Cadastrar conta\n(email/senha)"])
        UC01_2(["Cadastrar via OAuth2\n(Google/Apple)"])
        UC01_3(["Verificar e-mail"])
        UC01_4(["Fazer login\n(email/senha)"])
        UC01_5(["Fazer login\n(OAuth2)"])
        UC01_6(["Autenticar com 2FA\n(TOTP)"])
        UC01_7(["Recuperar senha"])
        UC01_8(["Configurar 2FA"])
        UC01_9(["Renovar access token\n(refresh token)"])
        UC01_10(["Revogar sessão\n(logout global)"])
        UC01_11(["Excluir conta"])
        UC01_12(["Verificar token expirado"])
    end

    %% Relacionamentos Titular
    Titular --> UC01_1
    Titular --> UC01_2
    Titular --> UC01_3
    Titular --> UC01_4
    Titular --> UC01_5
    Titular --> UC01_6
    Titular --> UC01_7
    Titular --> UC01_8
    Titular --> UC01_9
    Titular --> UC01_10
    Titular --> UC01_11

    %% Apoio também pode fazer login
    Apoio --> UC01_4
    Apoio --> UC01_2
    Apoio --> UC01_5
    Apoio --> UC01_9
    Apoio --> UC01_10

    %% Sistema
    Sistema --> UC01_12

    %% Inclusões (<<include>>)
    UC01_4 -. "<<include>>" .-> UC01_6
    %% Login inclui 2FA se ativo — RF-01.9

    %% Extensões (<<extend>>)
    UC01_1 -. "<<extend>>" .-> UC01_3
    %% Cadastro dispara verificação de e-mail
```

---

## UC-02: Vínculo Titular / Apoio

> Fluxo central de permissões. Garante que Apoio só veja o que Titular liberar.
> Revogação é imediata — sessão ativa do Apoio perde acesso em < 5s (RF-01.4).

```mermaid
flowchart LR
    Titular(["👤 Titular"])
    Apoio(["👥 Apoio"])
    Sistema(["⚙️ Sistema"])

    subgraph link ["🔗 Sistema: Link (Vínculo)"]
        UC02_1(["Criar convite\n(gerar link/token)"])
        UC02_2(["Aceitar convite\n(usar token 72h)"])
        UC02_3(["Revogar vínculo\nimediatamente"])
        UC02_4(["Conceder permissão\nde módulo"])
        UC02_5(["Revogar permissão\nde módulo"])
        UC02_6(["Visualizar lista\nde vínculos ativos"])
        UC02_7(["Ver log de acessos\ndo Apoio"])
        UC02_8(["Expirar convite\npassado TTL 72h"])
        UC02_9(["Invalidar sessões\ndo Apoio revogado"])
    end

    Titular --> UC02_1
    Titular --> UC02_3
    Titular --> UC02_4
    Titular --> UC02_5
    Titular --> UC02_6
    Titular --> UC02_7

    Apoio --> UC02_2

    Sistema --> UC02_8
    Sistema --> UC02_9

    %% Extensões
    UC02_3 -. "<<include>>" .-> UC02_9
    %% Revogar vínculo inclui invalidar sessões ativas

    UC02_1 -. "<<extend>>" .-> UC02_8
    %% Convite expirado é marcado pelo scheduler
```

---

## UC-03: Agenda e Planner

> Titular gerencia seus eventos e tarefas.
> Apoio visualiza apenas itens marcados como compartilhados + permissão AGENDA.

```mermaid
flowchart LR
    Titular(["👤 Titular"])
    Apoio(["👥 Apoio"])

    subgraph agenda ["📅 Sistema: Agenda"]
        UC03_1(["Criar evento\n(com/sem recorrência)"])
        UC03_2(["Criar tarefa\n(com subtarefas)"])
        UC03_3(["Editar evento recorrente\n(esta / futuras / todas)"])
        UC03_4(["Reagendar evento\n(drag-and-drop)"])
        UC03_5(["Concluir tarefa"])
        UC03_6(["Arquivar tarefa"])
        UC03_7(["Adicionar subtarefa"])
        UC03_8(["Configurar lembrete\n(simples ou persistente)"])
        UC03_9(["Visualizar agenda\n(dia/semana/mês)"])
        UC03_10(["Visualizar kanban\n(por status)"])
        UC03_11(["Visualizar lista\n(com filtros)"])
        UC03_12(["Criar categoria/tag\n(cor e ícone)"])
        UC03_13(["Aplicar template\nde rotina"])
        UC03_14(["Visualizar agenda\ndo Titular"])
    end

    Titular --> UC03_1
    Titular --> UC03_2
    Titular --> UC03_3
    Titular --> UC03_4
    Titular --> UC03_5
    Titular --> UC03_6
    Titular --> UC03_7
    Titular --> UC03_8
    Titular --> UC03_9
    Titular --> UC03_10
    Titular --> UC03_11
    Titular --> UC03_12
    Titular --> UC03_13

    Apoio --> UC03_14
    %% Apoio só acessa itens shared + permissão AGENDA ativa

    UC03_1 -. "<<extend>>" .-> UC03_8
    UC03_2 -. "<<extend>>" .-> UC03_7
    UC03_3 -. "<<include>>" .-> UC03_1
```

---

## UC-04: Foco e Produtividade

> Sessão de foco suprime notificações não críticas (RF-04.2).
> Titular controla totalmente o ciclo da sessão.

```mermaid
flowchart LR
    Titular(["👤 Titular"])
    Apoio(["👥 Apoio"])
    Sistema(["⚙️ Sistema"])

    subgraph focus ["🎯 Sistema: Focus"]
        UC04_1(["Iniciar sessão de foco\n(vincular tarefa opcional)"])
        UC04_2(["Pausar sessão"])
        UC04_3(["Retomar sessão"])
        UC04_4(["Finalizar sessão"])
        UC04_5(["Cancelar sessão"])
        UC04_6(["Ver histórico\nde sessões"])
        UC04_7(["Ver métricas\nde foco efetivo"])
        UC04_8(["Ver histórico\nde sessões (Apoio)"])
        UC04_9(["Suprimir notificações\ndurante sessão"])
        UC04_10(["Restaurar notificações\napós sessão"])
    end

    Titular --> UC04_1
    Titular --> UC04_2
    Titular --> UC04_3
    Titular --> UC04_4
    Titular --> UC04_5
    Titular --> UC04_6
    Titular --> UC04_7

    Apoio --> UC04_8
    %% Apoio vê histórico somente com permissão FOCUS_HISTORY

    Sistema --> UC04_9
    Sistema --> UC04_10

    UC04_1 -. "<<include>>" .-> UC04_9
    UC04_4 -. "<<include>>" .-> UC04_10
    UC04_5 -. "<<include>>" .-> UC04_10
```

---

## UC-05: Regulação Emocional

> **Dado sensível de saúde (LGPD art. 5º, II).**
> Apoio só acessa com permissão explícita do Titular.
> O produto nunca realiza diagnóstico clínico (RF-05).

```mermaid
flowchart LR
    Titular(["👤 Titular"])
    Apoio(["👥 Apoio"])
    Sistema(["⚙️ Sistema"])

    subgraph emotional ["💚 Sistema: Emotional"]
        UC05_1(["Registrar check-in\nde humor/energia"])
        UC05_2(["Escrever entrada\nno diário emocional"])
        UC05_3(["Ver dashboard\nde tendências"])
        UC05_4(["Exportar histórico\nemocional PDF/CSV"])
        UC05_5(["Ver check-ins\ndo Titular"])
        UC05_6(["Ver tendências\ndo Titular"])
        UC05_7(["Detectar padrão\nsustentado de baixa"])
        UC05_8(["Notificar Apoio\n(com consentimento)"])
    end

    Titular --> UC05_1
    Titular --> UC05_2
    Titular --> UC05_3
    Titular --> UC05_4

    Apoio --> UC05_5
    Apoio --> UC05_6
    %% Apenas com permissão EMOTIONAL_CHECKIN | JOURNAL

    Sistema --> UC05_7
    Sistema --> UC05_8
    %% RF-05.6: notifica Apoio após 5 dias consecutivos abaixo do limiar
    %% Apenas se Titular concedeu consentimento explícito

    UC05_7 -. "<<extend>>" .-> UC05_8
    %% Padrão sustentado pode estender para notificar Apoio
```

---

## UC-06: Cofre de Senhas (Vault Service)

> **Módulo isolado — zero-knowledge (RF-08, ADR-005).**
> Apoio não tem NENHUMA rota de acesso — restrição hard-coded.
> Decriptação ocorre exclusivamente no cliente.

```mermaid
flowchart LR
    Titular(["👤 Titular"])
    Sistema(["⚙️ Sistema"])

    subgraph vault ["🔒 Vault Service (isolado)"]
        UC06_1(["Configurar Cofre\n(definir senha mestra)"])
        UC06_2(["Abrir Cofre\n(autenticar com senha mestra)"])
        UC06_3(["Autenticar com biometria\n(atalho pós-setup)"])
        UC06_4(["Criar entrada\n(credencial/nota/cartão)"])
        UC06_5(["Editar entrada"])
        UC06_6(["Excluir entrada"])
        UC06_7(["Listar entradas\n(metadados, sem decifrar)"])
        UC06_8(["Gerar senha segura"])
        UC06_9(["Exportar backup\ncriptografado"])
        UC06_10(["Recuperar acesso\n(chave de recuperação)"])
        UC06_11(["Auto-lock do Cofre\nxmin de inatividade"])
    end

    Titular --> UC06_1
    Titular --> UC06_2
    Titular --> UC06_3
    Titular --> UC06_4
    Titular --> UC06_5
    Titular --> UC06_6
    Titular --> UC06_7
    Titular --> UC06_8
    Titular --> UC06_9
    Titular --> UC06_10

    Sistema --> UC06_11
    %% Auto-lock após N minutos de inatividade (padrão 5min — RF-08.6)

    UC06_2 -. "<<extend>>" .-> UC06_3
    %% Biometria como atalho após autenticação inicial com senha mestra

    UC06_1 -. "<<extend>>" .-> UC06_10
    %% Setup gera chave de recuperação exibida UMA VEZ (RF-08.11)

    note_apoio["❌ Conta de Apoio:\nSEM acesso ao Vault\nem nenhuma circunstância\n(RF-08.5 — hard-coded)"]
    style note_apoio fill:#ffcccc,stroke:#cc0000,color:#660000
```

---

## UC-07: Notificações e Lembretes

> Sistema de entrega multi-canal com fallback.
> Apoio pode disparar lembrete pontual se permissão SEND_REMINDERS ativa (RF-01.8).

```mermaid
flowchart LR
    Titular(["👤 Titular"])
    Apoio(["👥 Apoio"])
    Sistema(["⚙️ Sistema"])

    subgraph notif ["🔔 Sistema: Notificações"]
        UC07_1(["Configurar canal\n(push/email/SMS)"])
        UC07_2(["Configurar janela\nde silêncio"])
        UC07_3(["Confirmar\nlembrete recebido"])
        UC07_4(["Ver central\nde notificações"])
        UC07_5(["Enviar lembrete\npontual ao Titular"])
        UC07_6(["Disparar lembrete\nagendado"])
        UC07_7(["Disparar nudge\n(lembrete persistente)"])
        UC07_8(["Fallback: enviar\nvia e-mail se push falhou"])
        UC07_9(["Marcar lembrete como\n'não confirmado' no dashboard"])
    end

    Titular --> UC07_1
    Titular --> UC07_2
    Titular --> UC07_3
    Titular --> UC07_4

    Apoio --> UC07_5
    %% Apenas com permissão SEND_REMINDERS ativa — RF-01.8

    Sistema --> UC07_6
    Sistema --> UC07_7
    Sistema --> UC07_8
    Sistema --> UC07_9

    UC07_6 -. "<<extend>>" .-> UC07_7
    %% Lembrete agendado pode evoluir para nudge se não confirmado

    UC07_7 -. "<<extend>>" .-> UC07_8
    %% Nudge sem resposta pode fazer fallback por e-mail

    UC07_7 -. "<<extend>>" .-> UC07_9
    %% Após maxRepetitions sem confirmação — RF-03.2
```

---

## UC-08: Onboarding e Perfil

> Onboarding máximo 5 telas, dispensável (RF-10.3).
> Perfil declarado apenas sugere defaults — nunca restringe (RF-10).

```mermaid
flowchart LR
    Titular(["👤 Titular"])

    subgraph onboarding ["🚀 Sistema: Onboarding"]
        UC08_1(["Declarar perfil\nde neurodivergência"])
        UC08_2(["Pular onboarding\n(configurar depois)"])
        UC08_3(["Aplicar configurações\nsugeridas por perfil"])
        UC08_4(["Alterar perfil\ndeclarado"])
        UC08_5(["Reconfigurar\nsugestões de perfil"])
    end

    Titular --> UC08_1
    Titular --> UC08_2
    Titular --> UC08_4
    Titular --> UC08_5

    UC08_1 -. "<<extend>>" .-> UC08_3
    %% Perfil declarado sugere defaults (TDAH → nudge ativo, TEA → modo baixa densidade)
    %% Nunca obrigatório — Titular sempre pode sobrescrever
```

---

## UC-09: Administração Interna

> Acesso restrito à equipe técnica. Sem acesso a conteúdo sensível.
> Apenas metadados operacionais para suporte e auditoria de segurança.

```mermaid
flowchart LR
    Admin(["🛡️ Admin"])

    subgraph admin ["🏛️ Sistema: Admin"]
        UC09_1(["Ver status\noperacional de conta"])
        UC09_2(["Auditar acessos\nao Cofre"])
        UC09_3(["Ver health check\ne status page"])
        UC09_4(["Ver logs\nde tentativas negadas"])
        UC09_5(["Gerenciar fila\nde notificações"])
    end

    Admin --> UC09_1
    %% Apenas metadados (status, created_at) — NUNCA conteúdo de dados
    Admin --> UC09_2
    %% Apenas ng_vault_auth_logs — NUNCA ng_vault_entries (RF-12.4)
    Admin --> UC09_3
    Admin --> UC09_4
    Admin --> UC09_5

    note_admin["⚠️ Admin NUNCA acessa:\n• Conteúdo de tarefas/eventos\n• Dados emocionais\n• Diário\n• Entradas do Cofre\n• Notas de check-in"]
    style note_admin fill:#fff3cd,stroke:#856404,color:#533f03
```
