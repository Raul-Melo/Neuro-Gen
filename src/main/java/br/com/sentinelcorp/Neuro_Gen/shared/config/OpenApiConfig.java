package br.com.sentinelcorp.Neuro_Gen.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentação OpenAPI/Swagger (RNF-MAN.4). Define o esquema de autenticação Bearer (JWT) e
 * agrupa a documentação por módulo do monólito, refletindo os bounded contexts.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearer-jwt";

    @Bean
    public OpenAPI neuroGenOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Neuro-Gen API")
                        .description("Agenda-planner adaptativa para neurodivergentes — monólito modular "
                                + "(identity, link, agenda, focus, emotional) + Vault isolado.")
                        .version("v1")
                        .contact(new Contact().name("SentinelCorp").email("suporte@sentinelcorp.com.br"))
                        .license(new License().name("Proprietary")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token JWT obtido em POST /api/v1/auth/login")));
    }

    @Bean
    public GroupedOpenApi identityApi() {
        return GroupedOpenApi.builder().group("1-identity")
                .pathsToMatch("/api/v1/auth/**", "/api/v1/users/**").build();
    }

    @Bean
    public GroupedOpenApi linkApi() {
        return GroupedOpenApi.builder().group("2-link").pathsToMatch("/api/v1/links/**").build();
    }

    @Bean
    public GroupedOpenApi agendaApi() {
        return GroupedOpenApi.builder().group("3-agenda")
                .pathsToMatch("/api/v1/events/**", "/api/v1/tasks/**").build();
    }

    @Bean
    public GroupedOpenApi focusApi() {
        return GroupedOpenApi.builder().group("4-focus").pathsToMatch("/api/v1/focus/**").build();
    }

    @Bean
    public GroupedOpenApi emotionalApi() {
        return GroupedOpenApi.builder().group("5-emotional").pathsToMatch("/api/v1/emotional/**").build();
    }

    @Bean
    public GroupedOpenApi vaultApi() {
        return GroupedOpenApi.builder().group("6-vault").pathsToMatch("/api/v1/vault/**").build();
    }
}
