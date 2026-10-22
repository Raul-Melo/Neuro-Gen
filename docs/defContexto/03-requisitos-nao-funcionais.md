# Neuro-Gen — Requisitos Não Funcionais

**Versão:** 0.2 (aprofundado)
**Referência:** `01-visao-produto.md`, `02-requisitos-funcionais.md`
**Muda em relação à v0.1:** métricas mensuráveis por item, referência a padrões/normas (OWASP ASVS, WCAG 2.1, LGPD por artigo, ISO 27001), categorias novas (Observabilidade, Interoperabilidade, Retenção de Dados/DR detalhado, Internacionalização).

Numeração: `RNF-[CATEGORIA].[SEQ]`.

---

## RNF-SEC — Segurança

| ID | Requisito | Métrica/Referência |
|---|---|---|
| RNF-SEC.1 | TLS 1.3 obrigatório em todo tráfego cliente-servidor; TLS 1.2 apenas como fallback temporário | Sem exceção em produção |
| RNF-SEC.2 | Dados em repouso criptografados com AES-256-GCM | Aplicado a banco de dados e backups |
| RNF-SEC.3 | Cofre: criptografia zero-knowledge — chave derivada da senha mestra via PBKDF2/Argon2id no cliente; servidor armazena apenas payload cifrado | Servidor nunca detém chave de decriptação |
| RNF-SEC.4 | Isolamento lógico do serviço/banco do Cofre em relação aos demais módulos (schema/serviço separado, credenciais de acesso distintas) | Reduz raio de exposição em caso de breach parcial |
| RNF-SEC.5 | Hash de senha de login via Argon2id (parâmetros mínimos: m=19MB, t=2, p=1, conforme OWASP) | OWASP Password Storage Cheat Sheet |
| RNF-SEC.6 | Rate limiting em endpoints de autenticação e Cofre: máx. 5 tentativas/min por IP, bloqueio progressivo | Proteção contra brute-force/credential stuffing |
| RNF-SEC.7 | Sessão com expiração configurável (padrão 30 dias com refresh token) e revogação remota (logout global) | Usuário controla via configurações |
| RNF-SEC.8 | Log de auditoria imutável para: vínculo/revogação Titular-Apoio, alteração de permissão, acesso ao Cofre, tentativas de acesso negadas | Retenção mínima 12 meses |
| RNF-SEC.9 | Pentest externo antes de cada release major; revisão de dependências (SCA) contínua em CI | Mínimo 1x/ano em produção estável |
| RNF-SEC.10 | Alinhamento com OWASP ASVS nível 2 (aplicação que trata dados sensíveis) | Checklist de verificação por release |
| RNF-SEC.11 | Proteção contra OWASP Top 10 (injeção, broken auth, XSS, IDOR, etc.) validada em pipeline de CI (SAST) | Bloqueio de merge em vulnerabilidade crítica/alta |
| RNF-SEC.12 | Segregação de ambiente: dev/staging nunca usa dado real de produção sem anonimização | Obrigatório antes de qualquer carga de dados de teste |
| RNF-SEC.13 | Chaves de API/segredos de infraestrutura geridos via cofre de segredos de infraestrutura (ex: Vault/KMS), nunca em código-fonte | Verificado via secret scanning em CI |

## RNF-PRIV — Privacidade e Compliance (LGPD)

| ID | Requisito | Referência legal |
|---|---|---|
| RNF-PRIV.1 | Dado de neurodivergência e estado emocional tratado como dado sensível de saúde | LGPD art. 5º, II |
| RNF-PRIV.2 | Base legal de tratamento: consentimento explícito e específico do titular dos dados | LGPD art. 7º, I e art. 11 |
| RNF-PRIV.3 | Consentimento granular por categoria de dado compartilhada com Apoio, revogável a qualquer momento | LGPD art. 8º, §5º |
| RNF-PRIV.4 | Consentimento parental verificável para usuário menor de idade | LGPD art. 14; ECA |
| RNF-PRIV.5 | Direito de acesso, correção, portabilidade e eliminação de dados, self-service dentro do app | LGPD art. 18 |
| RNF-PRIV.6 | Prazo máximo de 15 dias para atendimento de solicitação de titular não automatizável via self-service | LGPD art. 19 |
| RNF-PRIV.7 | Minimização de dados: Apoio recebe apenas o subconjunto liberado, nunca o dado bruto completo por padrão | Princípio da minimização, LGPD art. 6º, III |
| RNF-PRIV.8 | Política de privacidade e termos versionados, aceite registrado com timestamp e versão | Rastreabilidade de consentimento |
| RNF-PRIV.9 | Relatório de Impacto à Proteção de Dados (RIPD/DPIA) elaborado antes do go-live, dado o volume de dado sensível | LGPD art. 38 (recomendado pela ANPD para dado sensível em escala) |
| RNF-PRIV.10 | Nomeação de Encarregado de Dados (DPO) com canal de contato público | LGPD art. 41 |
| RNF-PRIV.11 | Arquitetura de dados preparada para expansão a GDPR (base legal equivalente, direito ao esquecimento, DPA com subprocessadores) sem redesenho estrutural | Portabilidade de compliance internacional |
| RNF-PRIV.12 | Notificação à ANPD e aos titulares em caso de incidente de segurança com risco relevante | LGPD art. 48 |
| RNF-PRIV.13 | Contratos com subprocessadores (cloud, provedores de push/e-mail) com cláusulas de proteção de dados (DPA) | Obrigatório antes de qualquer contratação |

## RNF-PERF — Performance

| ID | Requisito | Métrica |
|---|---|---|
| RNF-PERF.1 | Tempo de resposta de API — leitura (agenda, tarefas) | p95 < 300ms, p99 < 800ms |
| RNF-PERF.2 | Tempo de resposta de API — escrita (criar/editar) | p95 < 500ms |
| RNF-PERF.3 | Tempo de carregamento inicial do app mobile (cold start) | < 2s em rede 4G, < 3.5s em 3G |
| RNF-PERF.4 | Tempo de carregamento do web app (First Contentful Paint) | < 1.8s em conexão banda larga padrão |
| RNF-PERF.5 | Sincronização entre dispositivos | < 5s em condição normal de rede |
| RNF-PERF.6 | Entrega de notificação push em relação ao horário agendado | Atraso máximo 30s (p95) |
| RNF-PERF.7 | Operação do Cofre (decriptação client-side de lista de entradas) | < 1s para até 200 entradas |
| RNF-PERF.8 | Consumo de bateria do app mobile em background (sync + notificações) | Não deve figurar entre os maiores consumidores do dispositivo em uso típico (validar em teste de campo) |

## RNF-USA — Usabilidade e Acessibilidade (crítico dado o perfil do usuário)

| ID | Requisito | Referência/Métrica |
|---|---|---|
| RNF-USA.1 | Baixa carga cognitiva: máx. 5–7 ações/elementos primários visíveis por tela | Heurística validada em teste de usabilidade com usuários neurodivergentes reais |
| RNF-USA.2 | Conformidade WCAG 2.1 nível AA | Contraste mínimo 4.5:1, navegação por teclado, foco visível, leitor de tela (VoiceOver/TalkBack) |
| RNF-USA.3 | Modo de estímulo sensorial reduzido: desativa animações, sons, cores saturadas | Ativável a qualquer momento, aplicado globalmente e imediatamente |
| RNF-USA.4 | Linguagem simples (nível de leitura acessível, evitar jargão e ambiguidade) em UI, notificações e mensagens de erro | Revisão de UX writing obrigatória por release |
| RNF-USA.5 | Tipografia amigável a dislexia como opção (ex: OpenDyslexic) e escala de tamanho de texto ajustável (100%–200%) | Configurável nas preferências |
| RNF-USA.6 | Onboarding máx. 5 telas, dispensável a qualquer momento, retomável | RF-10.3 |
| RNF-USA.7 | Confirmação explícita antes de ação destrutiva (exclusão, revogação de vínculo) | Sem exceção |
| RNF-USA.8 | Testes de usabilidade com usuários reais de cada perfil-alvo (TDAH, TEA, Superdotação) antes de cada release major | Mínimo 5 usuários por perfil, método qualitativo |
| RNF-USA.9 | Suporte a modo escuro/claro com contraste validado em ambos | WCAG AA em ambos os temas |
| RNF-USA.10 | Feedback tátil/visual imediato (< 100ms) para toda interação (toque, clique, drag) | Percepção de responsividade — reduz ansiedade de incerteza |

## RNF-DISP — Disponibilidade e Confiabilidade

| ID | Requisito | Métrica |
|---|---|---|
| RNF-DISP.1 | SLA de disponibilidade do backend | 99.5% mensal no MVP; meta 99.9% pós-MVP |
| RNF-DISP.2 | Backup automático diário | Retenção mínima 30 dias, teste de restauração trimestral |
| RNF-DISP.3 | RPO (Recovery Point Objective) | ≤ 24h |
| RNF-DISP.4 | RTO (Recovery Time Objective) | ≤ 4h |
| RNF-DISP.5 | Resolução de conflito de sincronização offline | Determinística, auditável, sem perda silenciosa de dado (RF-09.6) |
| RNF-DISP.6 | Circuit breaker e degradação graciosa: falha em serviço não crítico (ex: relatório) não pode derrubar core (agenda/lembretes) | Isolamento de falha por serviço |
| RNF-DISP.7 | Plano de disaster recovery documentado e testado (simulação) antes do go-live | Runbook formal |

## RNF-ESC — Escalabilidade

| ID | Requisito | Métrica |
|---|---|---|
| RNF-ESC.1 | Serviços backend stateless, escaláveis horizontalmente | Auto-scaling configurado por CPU/latência |
| RNF-ESC.2 | Banco de dados com estratégia de particionamento/sharding avaliada desde o schema inicial | Evitar redesenho em crescimento |
| RNF-ESC.3 | Sistema de notificações desacoplado via fila assíncrona (ex: SQS/RabbitMQ/Kafka) | Suporta pico de envio sem degradar API principal |
| RNF-ESC.4 | Capacidade-alvo inicial | Suportar 100k usuários ativos mensais sem redesenho arquitetural major |
| RNF-ESC.5 | Cache de leitura para dados de agenda/dashboard de alta frequência de acesso | Redis ou equivalente, TTL configurável |

## RNF-COMPAT — Compatibilidade Multiplataforma

| ID | Requisito |
|---|---|
| RNF-COMPAT.1 | Web: últimas 2 versões estáveis de Chrome, Safari, Firefox, Edge |
| RNF-COMPAT.2 | Android 10+ (API 29+) |
| RNF-COMPAT.3 | iOS 16+ |
| RNF-COMPAT.4 | Paridade funcional total entre plataformas para todo item Must (RF-09.1–09.3) |
| RNF-COMPAT.5 | Design responsivo cobrindo breakpoints mobile, tablet e desktop na web |

## RNF-MAN — Manutenibilidade e Qualidade de Código

| ID | Requisito | Métrica |
|---|---|---|
| RNF-MAN.1 | Cobertura de testes automatizados em módulos críticos (auth, cofre, sincronização, permissões) | ≥ 70% |
| RNF-MAN.2 | Cobertura de testes em demais módulos | ≥ 50% |
| RNF-MAN.3 | Pipeline CI/CD com deploy automatizado e rollback em < 10 min | Obrigatório antes de go-live |
| RNF-MAN.4 | Documentação de API (OpenAPI/Swagger) atualizada a cada release | Validação automática de contrato em CI |
| RNF-MAN.5 | Padrão de code review obrigatório (mínimo 1 aprovação) antes de merge em branch principal | Sem exceção |
| RNF-MAN.6 | Débito técnico rastreado e revisado em cadência definida (ver `agile-guide.md`) | Item de backlog, não invisível |

## RNF-OBS — Observabilidade (novo)

| ID | Requisito |
|---|---|
| RNF-OBS.1 | Logging estruturado centralizado, sem dado sensível em texto claro (PII/dado de saúde nunca em log) |
| RNF-OBS.2 | Monitoramento de métricas de sistema (latência, erro, throughput) com alerta automático em desvio de SLO |
| RNF-OBS.3 | Tracing distribuído para requisições cross-serviço (facilita diagnóstico em arquitetura de microsserviços, se adotada) |
| RNF-OBS.4 | Dashboard de saúde operacional acessível ao time técnico em tempo real |
| RNF-OBS.5 | Alerta específico para anomalias no Cofre (ex: pico de tentativas de acesso negadas) — tratado com prioridade de segurança, não apenas operacional |

## RNF-INTEROP — Interoperabilidade (novo)

| ID | Requisito |
|---|---|
| RNF-INTEROP.1 | Sincronização com calendários externos via padrões abertos (CalDAV/iCal) além de APIs proprietárias (Google/Microsoft) |
| RNF-INTEROP.2 | Exportação de dados em formato aberto (CSV/JSON/ICS) para portabilidade do usuário, independente de solicitação formal de LGPD |
| RNF-INTEROP.3 | API interna documentada e versionada, preparada para eventual abertura a integrações de terceiros (ex: plataformas terapêuticas) no roadmap futuro |

## RNF-I18N — Internacionalização (novo, preparação futura)

| ID | Requisito |
|---|---|
| RNF-I18N.1 | Arquitetura de texto preparada para múltiplos idiomas (strings externalizadas, sem hardcode), mesmo que MVP seja pt-BR único |
| RNF-I18N.2 | Formatação de data/hora/fuso horário abstraída por locale desde o início (evita retrabalho em expansão) |
| RNF-I18N.3 | Suporte a fuso horário do dispositivo com opção de fixar fuso manualmente (relevante para Apoio em fuso diferente do Titular) |

## RNF-RET — Retenção e Ciclo de Vida de Dados (novo, detalhamento de RNF-DISP.2/RNF-PRIV.5)

| ID | Requisito |
|---|---|
| RNF-RET.1 | Dados de conta excluída retidos por período mínimo legal (obrigações fiscais/regulatórias aplicáveis) e então eliminados de forma irreversível |
| RNF-RET.2 | Dados do Cofre de conta excluída eliminados de forma irreversível e prioritária, sem retenção estendida (dado não sujeito a obrigação legal de guarda) |
| RNF-RET.3 | Backups seguem o mesmo prazo de retenção/eliminação aplicado aos dados vivos, evitando "vazamento" de dado excluído via backup antigo |
| RNF-RET.4 | Política de retenção documentada e referenciada na política de privacidade (RNF-PRIV.8) |

---

## Observação de Arquitetura (atualizada)

Blocos que mais restringem decisões técnicas, em ordem de criticidade:

1. **RNF-SEC.3/SEC.4 (Cofre)** — define isolamento de serviço/schema e biblioteca de criptografia client-side compatível entre web, Android e iOS.
2. **RNF-PRIV (LGPD, dado sensível de saúde)** — exige DPIA/RIPD e modelagem de dados com privacy by design antes de qualquer schema final.
3. **RNF-USA (acessibilidade cognitiva)** — não é "nice to have": é requisito core que deve orientar decisão de framework de UI (capacidade de customização de densidade visual, temas, tipografia) tanto quanto requisito de performance.
4. **RNF-ESC.4 (100k MAU alvo)** — dimensiona escolha de banco de dados e estratégia de cache desde o MVP, mesmo que a carga inicial real seja muito menor.

Próximo artefato recomendado: ADR de stack técnica, já com este nível de detalhe de RNF como input direto de decisão (ex: escolha entre Kotlin Multiplatform/Flutter/React Native para mobile considerando RNF-USA.2 e RNF-COMPAT).
