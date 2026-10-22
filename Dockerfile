# syntax=docker/dockerfile:1
# Imagem de produção do Neuro-Gen (ADR-007). Build em dois estágios:
# o primeiro compila com Maven + JDK; o segundo só carrega o JAR sobre um JRE enxuto.
# Os testes NÃO rodam aqui: eles rodam no CI (GitHub Actions) antes do deploy.

# ---------- Estágio 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copiar só o pom primeiro permite reaproveitar o cache de dependências entre builds.
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline || true
COPY src ./src
RUN mvn -B -ntp -DskipTests package && cp target/*.jar app.jar

# ---------- Estágio 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Executa sem privilégios de root.
RUN addgroup -S neurogen && adduser -S neurogen -G neurogen
COPY --from=build /app/app.jar app.jar
USER neurogen

# JVM ajustada para contêineres pequenos (ex.: plano gratuito com 512 MB e CPU fracionada):
# heap proporcional à memória do contêiner, GC serial e JIT de nível 1 para subir mais rápido.
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
