package br.com.sentinelcorp.Neuro_Gen.identity.domain.exception;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ForbiddenAccessException;

/** HTTP 403 — tentativa de operação que exige e-mail verificado (RF-01.6). */
public class EmailNotVerifiedException extends ForbiddenAccessException {
    public EmailNotVerifiedException() {
        super("conta", "e-mail ainda não verificado");
    }
}
