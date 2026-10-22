package br.com.sentinelcorp.Neuro_Gen.shared.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * Infraestrutura do Spring Authorization Server reutilizada como provedor de identidade.
 *
 * <p>Expõe o {@link JWKSource} (par de chaves RSA), o {@link JwtEncoder} — usado pelo módulo
 * Identity para emitir os <i>access tokens</i> JWT de curta duração — e o {@link JwtDecoder}
 * consumido pelo Resource Server para validá-los. Os endpoints OAuth2 padrão (JWKS em
 * {@code /oauth2/jwks}) ficam disponíveis, tornando a aplicação um IdP válido.</p>
 *
 * <p><b>Nota de produção:</b> a chave RSA é gerada em memória para desenvolvimento. Em produção
 * deve vir de um cofre de segredos / rotação de chaves (RNF-SEC.13).</p>
 */
@Configuration
public class AuthorizationServerConfig {

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = generateRsa();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(NeuroGenProperties props) {
        return AuthorizationServerSettings.builder()
                .issuer(props.getJwt().getIssuer())
                .build();
    }

    /** Cliente first-party (web/mobile). Mantido para que o AS tenha um repositório válido. */
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient web = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("neurogen-web")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .build();
        return new InMemoryRegisteredClientRepository(web);
    }

    private static RSAKey generateRsa() {
        KeyPair keyPair = generateRsaKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    private static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao gerar par de chaves RSA", ex);
        }
    }
}
