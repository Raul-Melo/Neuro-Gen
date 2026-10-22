package br.com.sentinelcorp.Neuro_Gen.agenda.domain.exception;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.NotFoundException;

/** HTTP 404 — evento inexistente para este usuário. */
public class EventNotFoundException extends NotFoundException {
    public EventNotFoundException(String id) {
        super("Evento", id);
    }
}
