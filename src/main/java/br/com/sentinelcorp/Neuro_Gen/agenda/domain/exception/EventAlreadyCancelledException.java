package br.com.sentinelcorp.Neuro_Gen.agenda.domain.exception;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.DomainException;
import org.springframework.http.HttpStatus;

/** HTTP 409 — evento cancelado não aceita novas alterações. */
public class EventAlreadyCancelledException extends DomainException {

    public EventAlreadyCancelledException(String eventId) {
        super("EVENT_ALREADY_CANCELLED", "O evento " + eventId + " já está cancelado", HttpStatus.CONFLICT);
    }
}
