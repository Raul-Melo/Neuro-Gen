package br.com.sentinelcorp.Neuro_Gen.focus.domain.event;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/** Publicado ao encerrar a sessão — consumido para retomar a entrega normal de notificações. */
public record FocusSessionEnded(
        UUID eventId,
        Instant occurredAt,
        String aggregateId,
        String moduleName,
        String sessionId,
        String userId,
        String finalStatus) implements DomainEvent {

    public static FocusSessionEnded of(String sessionId, String userId, String finalStatus) {
        return new FocusSessionEnded(UUID.randomUUID(), Instant.now(), sessionId, "focus",
                sessionId, userId, finalStatus);
    }
}
