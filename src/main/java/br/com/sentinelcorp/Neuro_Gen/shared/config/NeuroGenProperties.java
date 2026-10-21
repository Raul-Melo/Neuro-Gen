package br.com.sentinelcorp.Neuro_Gen.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de aplicação (prefixo {@code neurogen.security}). Centraliza TTLs e chaves,
 * mantendo segredos fora do código em produção (RNF-SEC.13).
 */
@ConfigurationProperties(prefix = "neurogen.security")
public class NeuroGenProperties {

    private final Jwt jwt = new Jwt();
    private final Invite invite = new Invite();
    private final EmailVerification emailVerification = new EmailVerification();
    private final PasswordReset passwordReset = new PasswordReset();
    private String sensitiveDataKey;

    public Jwt getJwt() { return jwt; }
    public Invite getInvite() { return invite; }
    public EmailVerification getEmailVerification() { return emailVerification; }
    public PasswordReset getPasswordReset() { return passwordReset; }
    public String getSensitiveDataKey() { return sensitiveDataKey; }
    public void setSensitiveDataKey(String sensitiveDataKey) { this.sensitiveDataKey = sensitiveDataKey; }

    public static class Jwt {
        private String issuer = "https://neuro-gen.sentinelcorp.com.br";
        private long accessTokenTtlSeconds = 900;
        private long refreshTokenTtlSeconds = 2_592_000;

        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public long getAccessTokenTtlSeconds() { return accessTokenTtlSeconds; }
        public void setAccessTokenTtlSeconds(long v) { this.accessTokenTtlSeconds = v; }
        public long getRefreshTokenTtlSeconds() { return refreshTokenTtlSeconds; }
        public void setRefreshTokenTtlSeconds(long v) { this.refreshTokenTtlSeconds = v; }
    }

    public static class Invite {
        private int ttlHours = 72;
        public int getTtlHours() { return ttlHours; }
        public void setTtlHours(int ttlHours) { this.ttlHours = ttlHours; }
    }

    public static class EmailVerification {
        private int ttlHours = 24;
        public int getTtlHours() { return ttlHours; }
        public void setTtlHours(int ttlHours) { this.ttlHours = ttlHours; }
    }

    public static class PasswordReset {
        private int ttlMinutes = 15;
        public int getTtlMinutes() { return ttlMinutes; }
        public void setTtlMinutes(int ttlMinutes) { this.ttlMinutes = ttlMinutes; }
    }
}
