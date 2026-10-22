package br.com.sentinelcorp.Neuro_Gen.shared.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker/contrato para eventos de domínio.
 *
 * <p>Publicados pela camada de aplicação (após a persistência da operação) via
 * {@code ApplicationEventPublisher}, permitem comunicação <b>desacoplada</b> entre os módulos
 * internos do monólito modular — nenhum módulo importa classes de outro (ADR-001), a integração
 * acontece exclusivamente por estes eventos.</p>
 */
public interface DomainEvent {

    /** Identificador único do evento (idempotência / rastreio). */
    UUID eventId();

    /** Momento em que o fato ocorreu. */
    Instant occurredAt();

    /** Identificador do agregado que originou o evento. */
    String aggregateId();

    /** Nome do módulo de origem (ex.: "identity", "link"). */
    String moduleName();
}
