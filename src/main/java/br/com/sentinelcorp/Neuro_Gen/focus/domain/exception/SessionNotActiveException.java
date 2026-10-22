package br.com.sentinelcorp.Neuro_Gen.focus.domain.exception;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.UnprocessableEntityException;

/** HTTP 422 — operação inválida para o estado atual da sessão. */
public class SessionNotActiveException extends UnprocessableEntityException {
    public SessionNotActiveException(String detail) {
        super("SESSION_NOT_ACTIVE", detail);
    }
}
