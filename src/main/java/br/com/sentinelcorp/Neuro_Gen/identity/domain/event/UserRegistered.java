package br.com.sentinelcorp.Neuro_Gen.identity.domain.event;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/** Publicado quando uma conta é registrada. Consumível por onboarding/perfil, e-mail de boas-vindas. */
public record UserRegistered(
        UUID eventId,
        Instant occurredAt,
        String aggregateId,
        String moduleName,
        String userId,
        String email,
        String role) implements DomainEvent {

    public static UserRegistered of(String userId, String email, String role) {
        return new UserRegistered(UUID.randomUUID(), Instant.now(), userId, "identity", userId, email, role);
    }
}
