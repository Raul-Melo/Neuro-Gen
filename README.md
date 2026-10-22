<div align="center">
  <img src="./public/Neuro-Gen_v2.png" alt="Neuro-Gen Logo" width="160"/>

  <h1>Neuro-Gen</h1>

  <p><strong>Agenda personalizada para mentes neurodivergentes</strong></p>

  <p>
    <img src="https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk&logoColor=white"/>
    <img src="https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen?style=flat-square&logo=springboot&logoColor=white"/>
    <img src="https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square&logo=postgresql&logoColor=white"/>
    <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white"/>
    <img src="https://img.shields.io/badge/Status-Em%20Desenvolvimento-yellow?style=flat-square"/>
  </p>
  <p>
    <a href="https://github.com/Raul-Melo/Neuro-Gen/actions/workflows/ci.yml"><img src="https://github.com/Raul-Melo/Neuro-Gen/actions/workflows/ci.yml/badge.svg?branch=main" alt="CI"/></a>
  </p>
</div>

---

## Sobre o Projeto

**Neuro-Gen** é um software acadêmico desenvolvido para a disciplina **Projeto de Software 2**, com o objetivo de criar uma agenda digital personalizada e acessível para pessoas que possuem neurodivergências, incluindo:

- **TDAH** — Transtorno do Déficit de Atenção com Hiperatividade
- **TEA** — Transtorno do Espectro Autista
- **Superdotação** — Altas Habilidades / Superdotação

O Neuro-Gen visa empoderar seus usuários por meio de uma experiência de organização adaptada às suas necessidades cognitivas, promovendo melhoria em:

| Área | Objetivo |
|---|---|
| Desempenho | Melhorar a execução de tarefas e rotinas |
| Organização | Estruturar compromissos e prioridades de forma visual e intuitiva |
| Foco | Reduzir distrações com lembretes e fluxos simplificados |
| Performance | Acompanhar progresso e manter consistência ao longo do tempo |

---

## Stacks e Arquitetura

Backend em **monólito modular** com **arquitetura hexagonal** (Clean Architecture + DDD): 7 módulos
(`shared`, `identity`, `link`, `agenda`, `focus`, `emotional`, `vault`), cada um com as camadas
`domain → application → api → infrastructure`. As decisões estão registradas como ADRs em
[`docs/defContexto/05-adrs.md`](docs/defContexto/05-adrs.md) e o detalhamento do backend em
[`docs/backend/README.md`](docs/backend/README.md).

### Backend (ADR-002)

| Tecnologia | Versão | Papel |
|---|---|---|
| Java | 21 (LTS) | Linguagem principal |
| Spring Boot | 3.3.5 | Framework de aplicação (API REST) |
| Spring Security + OAuth2 Resource Server | 6.3 | Autenticação/autorização por JWT |
| Spring Authorization Server | 1.3 | Emissão e assinatura dos tokens (JWKS) |
| Spring Data JPA (Hibernate) | — | Persistência via adaptadores (portas do domínio) |
| Flyway | 10 | Versionamento do schema (Core e Vault separados) |
| springdoc-openapi | 2.6 | Documentação OpenAPI / Swagger UI |
| Bouncy Castle (Argon2id) · dev.samstevens.totp | — | Hash de senha e 2FA TOTP |
| Spring Boot Actuator | — | Health check / probes |
| Lombok | — | Redução de boilerplate |

### Banco de Dados

| Banco | Uso |
|---|---|
| PostgreSQL 16 | Datasource **Core** (schemas `ng_identity`, `ng_core`, `ng_sensitive`) e datasource **isolado do Cofre** (schema `ng_vault`, ADR-005) |

### Qualidade e Infraestrutura

| Ferramenta | Uso |
|---|---|
| JUnit 5 · AssertJ · Mockito · MockMvc | Testes de domínio, casos de uso e API (TDD — ADR-008) |
| Testcontainers | Teste de integração com PostgreSQL real |
| GitHub Actions | CI: build e toda a suíte de testes a cada push/PR |
| Docker · Docker Compose | Imagem de produção e banco local de desenvolvimento |
| Render | Hospedagem em nuvem (serviço Docker + PostgreSQL gerenciado — ADR-007) |
| Maven Wrapper | Build e gerenciamento de dependências |

---

## Primeiros Passos

### Pré-requisitos

- [Java 21](https://adoptium.net/)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- Maven (ou use o wrapper incluso `./mvnw`)

### Instalação e Execução

**1. Clone o repositório**
```bash
git clone https://github.com/Raul-Melo/Neuro-Gen.git
cd Neuro-Gen
```

**2. Suba os bancos de dados via Docker Compose**

O Spring Boot inicia os containers automaticamente ao rodar a aplicação em desenvolvimento. Caso prefira subir manualmente:
```bash
docker compose up -d
```

**3. Execute a aplicação**
```bash
./mvnw spring-boot:run
```

### Comandos Úteis

```bash
# Compilar e empacotar
./mvnw clean package

# Compilar sem executar testes
./mvnw clean package -DskipTests

# Executar todos os testes (o de integração precisa do Docker)
./mvnw test

# Executar uma classe de teste específica
./mvnw test -Dtest=NomeDaClasse
```

---

## Testes e TDD

```bash
./mvnw test     # unitários (domínio, casos de uso), API (MockMvc) e integração (precisa de Docker)
```

O desenvolvimento segue ciclos **red → green → refactor** registrados no histórico do Git. Para ver os
ciclos: `git log --oneline --grep=TDD`. Estratégia completa no **ADR-008**.

## Implantação em nuvem

A API roda em contêiner Docker no **Render**, com PostgreSQL 16 gerenciado. Todo merge na `main` com CI
verde é publicado automaticamente.

- **URL da API:** _preencher após o primeiro deploy_ (ex.: `https://neuro-gen-api.onrender.com`)
- **Swagger UI:** `<URL>/swagger-ui.html` · **Health:** `<URL>/actuator/health`
- Passo a passo e roteiro de demonstração: [`docs/deploy/README.md`](docs/deploy/README.md)

## Documentação

| Documento | Conteúdo |
|---|---|
| [`docs/defContexto/`](docs/defContexto/) | Visão do produto, requisitos funcionais e não funcionais, arquitetura e ADRs |
| [`docs/diagramas/01-diagrama-classes.md`](docs/diagramas/01-diagrama-classes.md) | Diagramas de classes por módulo |
| [`docs/diagramas/05-interacoes-componentes.md`](docs/diagramas/05-interacoes-componentes.md) | Diagramas de classes das interações entre componentes |
| [`docs/diagramas/`](docs/diagramas/) | DER, casos de uso e diagramas de sequência |
| [`docs/checkpoint-3/README.md`](docs/checkpoint-3/README.md) | Entregas do Checkpoint 3 e onde encontrar cada evidência |

---

## Equipe

<table align="center">
  <tr>
    <td align="center">
      <b>Raul Fernandes Silva Melo</b><br/>
      <sub>Desenvolvedor</sub>
    </td>
    <td align="center">
      <b>Bruna Duarte Bueno</b><br/>
      <sub>Desenvolvedora</sub>
    </td>
    <td align="center">
      <b>Matheus Lemes Carneiro</b><br/>
      <sub>Desenvolvedor</sub>
    </td>
  </tr>
</table>

<br/>

<div align="center">
  <b>Professor Supervisor:</b> Marcus Artiaga Colantoni
  <br/>
  <sub>Disciplina: Projeto de Software 2</sub>
</div>

---

<div align="center">
  <sub>Desenvolvido com dedicação por <strong>SentinelCorp</strong> &mdash; 2025</sub>
</div>
