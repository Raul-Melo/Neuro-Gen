package br.com.sentinelcorp.Neuro_Gen.identity.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;

import java.time.Instant;
import java.util.UUID;

/**
 * Sessão de refresh token persistida (tabela {@code ng_sessions}). O access token JWT é stateless
 * e não é persistido; apenas o hash do refresh token opaco fica no banco (RNF-SEC.7).
 */
public record RefreshSession(
        UUID id,
        UserId userId,
        String refreshTokenHash,
        Instant expiresAt,
        String deviceInfo,
        Instant createdAt,
        Instant revokedAt) {

    public static RefreshSession issue(UserId userId, String refreshTokenHash, Instant expiresAt, String deviceInfo) {
        return new RefreshSession(UUID.randomUUID(), userId, refreshTokenHash, expiresAt, deviceInfo,
                Instant.now(), null);
    }

    public boolean isActive() {
        return revokedAt == null && expiresAt.isAfter(Instant.now());
    }
}
