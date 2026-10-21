package br.com.sentinelcorp.Neuro_Gen.link.domain.event;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/** Publicado quando um convite de vínculo é criado. */
public record LinkCreated(
        UUID eventId,
        Instant occurredAt,
        String aggregateId,
        String moduleName,
        String linkId,
        String titularId) implements DomainEvent {

    public static LinkCreated of(String linkId, String titularId) {
        return new LinkCreated(UUID.randomUUID(), Instant.now(), linkId, "link", linkId, titularId);
    }
}
