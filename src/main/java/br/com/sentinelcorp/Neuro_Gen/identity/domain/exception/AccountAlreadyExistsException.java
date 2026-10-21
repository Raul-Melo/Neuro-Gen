package br.com.sentinelcorp.Neuro_Gen.identity.domain.exception;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ConflictException;

/** HTTP 409 — e-mail já cadastrado. */
public class AccountAlreadyExistsException extends ConflictException {
    public AccountAlreadyExistsException(String email) {
        super("Conta", "e-mail já cadastrado: " + email);
    }
}
