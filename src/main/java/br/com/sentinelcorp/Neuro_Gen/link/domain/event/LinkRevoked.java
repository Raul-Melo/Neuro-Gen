package br.com.sentinelcorp.Neuro_Gen.link.domain.event;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Publicado quando um vínculo é revogado (RF-01.4). Consumido pelo módulo Identity para invalidar
 * imediatamente as sessões ativas do Apoio (propagação &lt; 5s). É contrato público do módulo Link.
 */
public record LinkRevoked(
        UUID eventId,
        Instant occurredAt,
        String aggregateId,
        String moduleName,
        String linkId,
        String titularId,
        String apoioId) implements DomainEvent {

    public static LinkRevoked of(String linkId, String titularId, String apoioId) {
        return new LinkRevoked(UUID.randomUUID(), Instant.now(), linkId, "link", linkId, titularId, apoioId);
    }
}
