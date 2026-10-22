package br.com.sentinelcorp.Neuro_Gen.identity.domain.event;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/** Publicado quando o usuário verifica o e-mail — libera funcionalidades além de leitura (RF-01.6). */
public record UserEmailVerified(
        UUID eventId,
        Instant occurredAt,
        String aggregateId,
        String moduleName,
        String userId) implements DomainEvent {

    public static UserEmailVerified of(String userId) {
        return new UserEmailVerified(UUID.randomUUID(), Instant.now(), userId, "identity", userId);
    }
}
