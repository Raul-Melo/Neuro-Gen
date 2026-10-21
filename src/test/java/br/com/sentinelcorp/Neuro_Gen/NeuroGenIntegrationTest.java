package br.com.sentinelcorp.Neuro_Gen;

import br.com.sentinelcorp.Neuro_Gen.vault.infrastructure.persistence.VaultAuthLogJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração de ponta a ponta com PostgreSQL real (Testcontainers). Verifica que o
 * contexto sobe com os dois datasources + Flyway (core e vault) e exerce o teste de segurança
 * obrigatório: uma conta APOIO recebe 403 no Cofre e a tentativa é auditada (RF-08.5).
 *
 * <p>Pulado automaticamente quando o Docker não está disponível (ex.: máquina local sem Docker).</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class NeuroGenIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("neurogen")
            .withUsername("neurogen")
            .withPassword("secret");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.docker.compose.enabled", () -> "false");
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // O datasource isolado do Vault aponta para a mesma instância no teste (schema ng_vault).
        registry.add("vault.datasource.url", postgres::getJdbcUrl);
        registry.add("vault.datasource.username", postgres::getUsername);
        registry.add("vault.datasource.password", postgres::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtEncoder jwtEncoder;

    @Autowired
    VaultAuthLogJpaRepository vaultAuthLogs;

    @Test
    void contextLoadsAndMigrationsApply() {
        // Se chegou aqui, os dois datasources + Flyway (core e vault) subiram com sucesso.
        assertThat(jwtEncoder).isNotNull();
    }

    @Test
    void apoioAccountIsDeniedAccessToVaultAndAttemptIsAudited() throws Exception {
        String apoioToken = mintToken("APOIO");

        mockMvc.perform(get("/api/v1/vault/entries")
                        .header("Authorization", "Bearer " + apoioToken)
                        .header("X-Vault-Session", "irrelevant"))
                .andExpect(status().isForbidden());

        // A tentativa negada deve ter sido registrada em ng_vault_auth_logs (RF-08.5 / RNF-SEC.8).
        assertThat(vaultAuthLogs.count()).isGreaterThan(0);
    }

    private String mintToken(String role) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("https://neuro-gen.sentinelcorp.com.br")
                .issuedAt(now)
                .expiresAt(now.plus(15, ChronoUnit.MINUTES))
                .subject(UUID.randomUUID().toString())
                .claim("role", role)
                .claim("email_verified", true)
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
