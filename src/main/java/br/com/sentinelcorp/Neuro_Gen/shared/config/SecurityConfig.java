package br.com.sentinelcorp.Neuro_Gen.shared.config;

import br.com.sentinelcorp.Neuro_Gen.shared.security.CurrentUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * Configuração de segurança em três cadeias de filtro ordenadas:
 * <ol>
 *   <li><b>@Order(1)</b> endpoints do Authorization Server (OAuth2/JWKS);</li>
 *   <li><b>@Order(2)</b> Vault — matcher dedicado que NEGA estruturalmente qualquer role APOIO
 *       (RF-08.5), antes de qualquer checagem de permissão configurável;</li>
 *   <li><b>@Order(3)</b> API geral — endpoints públicos liberados, demais exigem JWT válido.</li>
 * </ol>
 * Stateless (JWT) em todas — sem sessão de servidor (RNF-ESC.1).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/password-reset/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health/**",
            "/oauth2/jwks"
    };

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain vaultSecurityFilterChain(
            HttpSecurity http,
            org.springframework.security.web.access.AccessDeniedHandler vaultAccessDeniedHandler) throws Exception {
        http.securityMatcher("/api/v1/vault/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Negação ESTRUTURAL do papel Apoio ao Cofre (RF-08.5 / ADR-005) — não é checagem
                // configurável: APOIO nunca autoriza nesta cadeia.
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("TITULAR"))
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                // Tentativa negada (ex.: APOIO) é auditada em ng_vault_auth_logs (RF-08.5 / RNF-SEC.8).
                .exceptionHandling(ex -> ex.accessDeniedHandler(vaultAccessDeniedHandler));
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    /** Converte o claim {@code role} do JWT em uma authority {@code ROLE_<role>}. */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var authorities = new java.util.ArrayList<>(scopes.convert(jwt));
            String role = jwt.getClaimAsString(CurrentUser.ROLE_CLAIM);
            if (role != null && !role.isBlank()) {
                authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role));
            }
            return authorities;
        });
        return converter;
    }

    /** Argon2id com parâmetros OWASP (m=19MB, t=2, p=1) — RNF-SEC.5. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // saltLength=16, hashLength=32, parallelism=1, memory=19456 KiB (~19MB), iterations=2
        return new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
    }

    static List<String> publicEndpoints() {
        return List.of(PUBLIC_ENDPOINTS);
    }
}
