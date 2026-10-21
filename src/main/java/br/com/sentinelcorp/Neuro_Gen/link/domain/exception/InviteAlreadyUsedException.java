package br.com.sentinelcorp.Neuro_Gen.link.domain.exception;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ConflictException;

/** HTTP 409 — convite já foi aceito anteriormente. */
public class InviteAlreadyUsedException extends ConflictException {
    public InviteAlreadyUsedException() {
        super("Convite", "já utilizado");
    }
}
