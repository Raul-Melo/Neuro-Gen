package br.com.sentinelcorp.Neuro_Gen.link.domain.exception;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ConflictException;

/** HTTP 409 — vínculo já foi revogado. */
public class LinkAlreadyRevokedException extends ConflictException {
    public LinkAlreadyRevokedException() {
        super("Vínculo", "já revogado");
    }
}
