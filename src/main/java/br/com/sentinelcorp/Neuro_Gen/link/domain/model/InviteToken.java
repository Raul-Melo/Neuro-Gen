package br.com.sentinelcorp.Neuro_Gen.link.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;
import br.com.sentinelcorp.Neuro_Gen.shared.security.Tokens;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Token de convite com expiração (TTL 72h — RF-01.2). O valor em texto claro é devolvido ao Titular
 * para compartilhar; apenas o seu hash SHA-256 ({@link #toHash()}) é persistido.
 */
public record InviteToken(String value, Instant expiresAt) {

    public InviteToken {
        if (value == null || value.isBlank() || expiresAt == null) {
            throw new ValidationException(Map.of("inviteToken", "token inválido"));
        }
    }

    public static InviteToken generate(Duration ttl) {
        return new InviteToken(Tokens.random(24), Instant.now().plus(ttl));
    }

    /** Reconstrói mantendo apenas a expiração (sem o valor em claro, já que só o hash é persistido). */
    public static InviteToken fromPersistence(Instant expiresAt) {
        return new InviteToken("<persisted>", expiresAt);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public String toHash() {
        return Tokens.sha256Hex(value);
    }
}
