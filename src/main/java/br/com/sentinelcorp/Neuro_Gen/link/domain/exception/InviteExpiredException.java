package br.com.sentinelcorp.Neuro_Gen.link.domain.exception;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.GoneException;

/** HTTP 410 — convite expirou (> 72h). O Titular deve gerar um novo. */
public class InviteExpiredException extends GoneException {
    public InviteExpiredException() {
        super("Convite", "expirado — gere um novo convite");
    }
}
