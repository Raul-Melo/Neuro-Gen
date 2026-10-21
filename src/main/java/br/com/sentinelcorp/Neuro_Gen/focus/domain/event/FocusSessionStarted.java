package br.com.sentinelcorp.Neuro_Gen.focus.domain.event;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/** Publicado ao iniciar a sessão — consumido para suprimir notificações não críticas (RF-04.2). */
public record FocusSessionStarted(
        UUID eventId,
        Instant occurredAt,
        String aggregateId,
        String moduleName,
        String sessionId,
        String userId) implements DomainEvent {

    public static FocusSessionStarted of(String sessionId, String userId) {
        return new FocusSessionStarted(UUID.randomUUID(), Instant.now(), sessionId, "focus", sessionId, userId);
    }
}
