package br.com.sentinelcorp.Neuro_Gen.identity.domain.exception;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ForbiddenAccessException;

/** HTTP 403 — conta suspensa, não pode autenticar. */
public class AccountSuspendedException extends ForbiddenAccessException {
    public AccountSuspendedException() {
        super("conta", "conta suspensa");
    }
}
