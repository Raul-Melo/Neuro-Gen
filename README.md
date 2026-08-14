<div align="center">
  <img src="./public/Neuro-Gen_v2.png" alt="Neuro-Gen Logo" width="160"/>

  <h1>Neuro-Gen</h1>

  <p><strong>Agenda personalizada para mentes neurodivergentes</strong></p>

  <p>
    <img src="https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk&logoColor=white"/>
    <img src="https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen?style=flat-square&logo=springboot&logoColor=white"/>
    <img src="https://img.shields.io/badge/PostgreSQL-blue?style=flat-square&logo=postgresql&logoColor=white"/>
    <img src="https://img.shields.io/badge/SQL%20Server-red?style=flat-square&logo=microsoftsqlserver&logoColor=white"/>
    <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white"/>
    <img src="https://img.shields.io/badge/Status-Em%20Desenvolvimento-yellow?style=flat-square"/>
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

> A definição completa das stacks e da arquitetura está em andamento. As tecnologias listadas abaixo representam o ambiente de backend já configurado.

### Backend

| Tecnologia | Versão | Papel |
|---|---|---|
| Java | 21 | Linguagem principal |
| Spring Boot | 4.1.0 | Framework de aplicação |
| Spring Security | — | Autenticação e autorização |
| OAuth2 Authorization Server | — | Servidor de identidade |
| OAuth2 Client | — | Integração com provedores externos |
| Spring Data JPA | — | Camada ORM / entidades |
| Spring Data JDBC | — | Acesso SQL direto e leve |
| Spring Session JDBC | — | Persistência de sessões no banco |
| Spring RestClient | — | Comunicação HTTP com serviços externos |
| Lombok | — | Redução de boilerplate |

### Banco de Dados

| Banco | Uso |
|---|---|
| PostgreSQL | Banco de dados principal |
| Microsoft SQL Server | Banco de dados alternativo / legado |

### Infraestrutura

| Ferramenta | Uso |
|---|---|
| Docker & Docker Compose | Orquestração local dos bancos de dados |
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

# Executar todos os testes
./mvnw test

# Executar uma classe de teste específica
./mvnw test -Dtest=NomeDaClasse
```

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
