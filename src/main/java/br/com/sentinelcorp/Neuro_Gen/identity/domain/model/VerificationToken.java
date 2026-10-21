package br.com.sentinelcorp.Neuro_Gen.identity.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;

import java.time.Instant;
import java.util.UUID;

/**
 * Token de uso único com TTL, usado tanto para verificação de e-mail (TTL 24h) quanto para
 * redefinição de senha (TTL 15min — RF-01.7). Persistido apenas como hash.
 */
public record VerificationToken(
        UUID id,
        UserId userId,
        String tokenHash,
        Instant expiresAt,
        Instant usedAt,
        Instant createdAt) {

    public static VerificationToken issue(UserId userId, String tokenHash, Instant expiresAt) {
        return new VerificationToken(UUID.randomUUID(), userId, tokenHash, expiresAt, null, Instant.now());
    }

    public boolean isUsable() {
        return usedAt == null && expiresAt.isAfter(Instant.now());
    }
}
