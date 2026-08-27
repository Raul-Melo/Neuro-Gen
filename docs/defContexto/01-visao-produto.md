# Neuro-Gen — Documento de Visão de Produto

**Versão:** 0.1 (Draft inicial)
**Status:** Em definição
**Owner:** PO/Arquitetura

---

## 1. Problema

Aplicativos de agenda genéricos assumem função executiva típica: o usuário lembra de checar a agenda, prioriza sozinho, e não sofre sobrecarga sensorial com listas densas ou notificações mal calibradas. Para pessoas com TDAH, TEA, Superdotação e outras condições de neurodivergência, isso falha em três pontos:

1. **Função executiva**: dificuldade em iniciar tarefas, estimar tempo, e manter atenção sem estímulo externo constante.
2. **Regulação emocional**: sobrecarga, ansiedade e burnout não são capturados nem mitigados pela ferramenta.
3. **Suporte externo**: cuidadores/responsáveis (pais, terapeutas, parceiros) não têm visibilidade ou forma de apoiar sem invadir a autonomia do usuário.

## 2. Visão

Neuro-Gen é uma agenda-planner adaptativa para neurodivergentes, que combina organização, lembretes ativos, ferramentas de foco e regulação emocional, com acesso compartilhado controlado entre o usuário titular e sua rede de apoio — em um único produto, disponível em web e mobile (Android/iOS).

## 3. Objetivos de Negócio

| Objetivo | Métrica de sucesso |
|---|---|
| Reduzir abandono de tarefas por falha de função executiva | % de tarefas concluídas vs. criadas |
| Aumentar adesão diária | DAU/MAU, streak de check-ins |
| Reduzir sobrecarga emocional não percebida | Frequência de check-ins emocionais preenchidos |
| Viabilizar suporte familiar/terapêutico sem invasão de privacidade | % de contas com apoio vinculado ativo |
| Reter usuários no plano pago (cofre + personalização avançada) | Taxa de conversão free → pago |

## 4. Personas

### 4.1 Titular (usuário primário — neurodivergente)
- Pode ser adolescente, adulto jovem ou adulto.
- Diagnóstico ou autopercepção de TDAH, TEA, Superdotação, ou combinação (ex: 2E — twice exceptional).
- Dores: esquecimento de compromissos, procrastinação, hiperfoco descontrolado, sobrecarga sensorial, dificuldade em quebrar tarefas grandes.
- Precisa de: autonomia, controle sobre o que é compartilhado, baixa carga cognitiva na interface.

### 4.2 Apoio (usuário secundário — rede de suporte)
- Pai/mãe/responsável, terapeuta, parceiro(a), coach.
- Acessa mediante vínculo aprovado pelo Titular (ou pelo responsável legal, se o Titular for menor de idade).
- Dores: falta de visibilidade sobre rotina e estado emocional do Titular sem invadir privacidade; dificuldade em reforçar hábitos de fora.
- Precisa de: visão configurável (o que o Titular decide compartilhar), forma de enviar lembretes/incentivos sem microgerenciar.

## 5. Modelo de Contas e Vínculo (decisão de arquitetura de produto)

- Conta **Titular**: dona dos dados. Define granularmente o que é visível para cada conta de Apoio vinculada (ex: agenda sim, diário emocional não).
- Conta **Apoio**: vinculada via convite (código/link) aceito pelo Titular. Sem acesso ao Cofre de Senhas do Titular em nenhuma hipótese — módulo estritamente pessoal e intransferível.
- Caso de menor de idade: fluxo de consentimento parental na criação da conta (obrigatório para compliance — ver RNF de privacidade). Responsável legal tem permissão elevada por padrão, ajustável conforme idade/autonomia definida no app.
- Multiplicidade: um Titular pode ter N contas de Apoio vinculadas; uma conta de Apoio pode estar vinculada a N Titulares (ex: terapeuta com múltiplos pacientes).

## 6. Escopo

### 6.1 Dentro do escopo (visão de produto completa)
- Agenda/planner personalizável (visões: dia, semana, kanban, lista)
- Lembretes constantes e adaptativos (multi-canal: push, e-mail, alarmes recorrentes)
- Ferramentas de foco (timers, modo foco, técnicas tipo Pomodoro adaptado)
- Regulação emocional (check-in de humor/energia, diário, insights)
- Organização (tarefas, subtarefas, tags, categorização por contexto)
- Otimização/performance (dashboard de progresso, padrões de produtividade)
- Acesso compartilhado Titular/Apoio com permissões granulares
- Cofre de senhas e segredos (módulo isolado, criptografado)
- Web app + apps nativos/híbridos Android e iOS

### 6.2 Fora do escopo (nesta fase — v1)
- Diagnóstico clínico ou substituição de acompanhamento terapêutico/médico
- Comunicação em tempo real tipo chat entre Titular e Apoio (pode virar épico futuro)
- Integração com prontuário eletrônico/sistemas de saúde
- Gamificação social pública (rankings, comparação entre usuários)

## 7. Restrições Conhecidas

- Dado sensível: informação de neurodivergência e estado emocional é **dado sensível de saúde** sob a LGPD (art. 5º, II) — exige tratamento reforçado (ver documento de RNF).
- Produto multiplataforma desde o MVP (web + Android + iOS) — decisão de stack deve favorecer código compartilhado (ver ADR a ser produzido na fase de arquitetura).
- Cofre de senhas é superfície de altíssima criticidade de segurança — não pode compartilhar infraestrutura de auth do restante do app sem isolamento adicional.

## 8. Próximos artefatos

1. `02-requisitos-funcionais.md` — Épicos, features e critérios de aceitação.
2. `03-requisitos-nao-funcionais.md` — Segurança, privacidade, performance, usabilidade, compliance.
3. Roadmap de MVP (priorização MoSCoW) — incluso no documento de requisitos funcionais.
4. ADR de stack técnica (web + mobile + backend) — próxima etapa, após validação deste escopo.
