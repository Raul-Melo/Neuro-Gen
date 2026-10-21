package br.com.sentinelcorp.Neuro_Gen.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades do serviço isolado de Vault (prefixo {@code vault}). Datasource e TTL de sessão
 * do cofre (auto-lock — RF-08.6) ficam separados do resto da aplicação (ADR-005).
 */
@ConfigurationProperties(prefix = "vault")
public class VaultProperties {

    private final Session session = new Session();

    public Session getSession() { return session; }

    public static class Session {
        private long ttlSeconds = 300;
        public long getTtlSeconds() { return ttlSeconds; }
        public void setTtlSeconds(long ttlSeconds) { this.ttlSeconds = ttlSeconds; }
    }
}
