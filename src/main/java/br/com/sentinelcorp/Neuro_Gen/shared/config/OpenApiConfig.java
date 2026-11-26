package br.com.sentinelcorp.Neuro_Gen.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Documentação OpenAPI/Swagger (RNF-MAN.4).
 *
 * <p>Acesse a UI em: {@code /swagger-ui.html} | Spec JSON em: {@code /v3/api-docs}</p>
 *
 * <p>Grupos por módulo refletem os bounded contexts do monólito modular:</p>
 * <ul>
 *   <li>1-identity — autenticação, cadastro, 2FA (TOTP), perfil</li>
 *   <li>2-link — vínculos Titular/Apoio e permissões granulares</li>
 *   <li>3-agenda — eventos (RRULE), tarefas (subtarefas) e lembretes</li>
 *   <li>4-focus — sessões Pomodoro com pausas cronometradas</li>
 *   <li>5-emotional — check-ins de humor e diário cifrado</li>
 *   <li>6-vault — cofre zero-knowledge em datasource isolado</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearer-jwt";

    @Bean
    public OpenAPI neuroGenOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Neuro-Gen API")
                        .version("v1.0")
                        .description("""
                                **Neuro-Gen** é uma agenda-planner adaptativa para neurodivergentes, \
                                desenvolvida pela SentinelCorp.

                                ### Autenticação
                                Todos os endpoints (exceto `/auth/login` e `/auth/register`) exigem \
                                `Authorization: Bearer <access_token>` obtido em `POST /api/v1/auth/login`.

                                ### Tokens
                                - **Access token**: validade curta (padrão 15 min), enviado no header `Authorization`.
                                - **Refresh token**: validade longa, renovado via `POST /api/v1/auth/refresh`.

                                ### Erros — RFC 7807
                                Todos os erros seguem o formato Problem Details:
                                ```json
                                {
                                  "type": "about:blank",
                                  "title": "Conflict",
                                  "status": 409,
                                  "detail": "...",
                                  "instance": "/api/v1/..."
                                }
                                ```

                                ### Módulos
                                | Módulo | Base path | Descrição |
                                |--------|-----------|-----------|
                                | identity | `/api/v1/auth`, `/api/v1/users` | Registro, login (JWT + TOTP), perfil |
                                | link | `/api/v1/links` | Vínculos Titular/Apoio e permissões |
                                | agenda | `/api/v1/events`, `/api/v1/tasks` | Eventos, tarefas e lembretes |
                                | focus | `/api/v1/focus` | Sessões Pomodoro com pausas |
                                | emotional | `/api/v1/emotional` | Check-ins de humor e diário cifrado |
                                | vault | `/api/v1/vault` | Cofre zero-knowledge (apenas Titular) |
                                """)
                        .contact(new Contact()
                                .name("SentinelCorp — Suporte")
                                .email("suporte@sentinelcorp.com.br"))
                        .license(new License()
                                .name("Proprietário — uso restrito")
                                .url("https://sentinelcorp.com.br/termos")))
                .externalDocs(new ExternalDocumentation()
                        .description("Arquitetura e ADRs do projeto Neuro-Gen")
                        .url("https://github.com/Raul-Melo/Neuro-Gen"))
                .servers(List.of(
                        new Server().url("/").description("Servidor atual"),
                        new Server().url("https://neuro-gen.onrender.com").description("Produção (Render)")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components()
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token JWT obtido em `POST /api/v1/auth/login`. "
                                           + "Envie no header: `Authorization: Bearer <token>`"))
                        .addResponses("Unauthorized", unauthorizedResponse())
                        .addResponses("Forbidden", forbiddenResponse())
                        .addResponses("NotFound", notFoundResponse())
                        .addResponses("Conflict", conflictResponse())
                        .addResponses("UnprocessableEntity", unprocessableResponse()));
    }

    // ── Grupos por módulo ────────────────────────────────────────────────────

    @Bean
    public GroupedOpenApi identityApi() {
        return GroupedOpenApi.builder()
                .group("1-identity")
                .displayName("Identity — Auth, Cadastro e 2FA")
                .pathsToMatch("/api/v1/auth/**", "/api/v1/users/**")
                .build();
    }

    @Bean
    public GroupedOpenApi linkApi() {
        return GroupedOpenApi.builder()
                .group("2-link")
                .displayName("Link — Vínculos Titular/Apoio")
                .pathsToMatch("/api/v1/links/**")
                .build();
    }

    @Bean
    public GroupedOpenApi agendaApi() {
        return GroupedOpenApi.builder()
                .group("3-agenda")
                .displayName("Agenda — Eventos, Tarefas e Lembretes")
                .pathsToMatch("/api/v1/events/**", "/api/v1/tasks/**")
                .build();
    }

    @Bean
    public GroupedOpenApi focusApi() {
        return GroupedOpenApi.builder()
                .group("4-focus")
                .displayName("Focus — Sessões Pomodoro")
                .pathsToMatch("/api/v1/focus/**")
                .build();
    }

    @Bean
    public GroupedOpenApi emotionalApi() {
        return GroupedOpenApi.builder()
                .group("5-emotional")
                .displayName("Emotional — Humor e Diário")
                .pathsToMatch("/api/v1/emotional/**")
                .build();
    }

    @Bean
    public GroupedOpenApi vaultApi() {
        return GroupedOpenApi.builder()
                .group("6-vault")
                .displayName("Vault — Cofre Zero-Knowledge")
                .pathsToMatch("/api/v1/vault/**")
                .build();
    }

    // ── Respostas de erro reutilizáveis (RFC 7807) ───────────────────────────

    @SuppressWarnings("rawtypes")
    private static ApiResponse unauthorizedResponse() {
        return new ApiResponse()
                .description("Token JWT ausente, inválido ou expirado")
                .content(new Content().addMediaType("application/problem+json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))));
    }

    @SuppressWarnings("rawtypes")
    private static ApiResponse forbiddenResponse() {
        return new ApiResponse()
                .description("Acesso negado — papel insuficiente ou recurso não pertence ao usuário")
                .content(new Content().addMediaType("application/problem+json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))));
    }

    @SuppressWarnings("rawtypes")
    private static ApiResponse notFoundResponse() {
        return new ApiResponse()
                .description("Recurso não encontrado")
                .content(new Content().addMediaType("application/problem+json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))));
    }

    @SuppressWarnings("rawtypes")
    private static ApiResponse conflictResponse() {
        return new ApiResponse()
                .description("Conflito — recurso já existe ou regra de negócio violada")
                .content(new Content().addMediaType("application/problem+json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))));
    }

    @SuppressWarnings("rawtypes")
    private static ApiResponse unprocessableResponse() {
        return new ApiResponse()
                .description("Entidade não processável — validação de domínio falhou")
                .content(new Content().addMediaType("application/problem+json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))));
    }
}
