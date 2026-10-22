package br.com.sentinelcorp.Neuro_Gen.focus.domain.exception;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ConflictException;

/** HTTP 409 — o usuário já possui uma sessão de foco em andamento. */
public class SessionAlreadyActiveException extends ConflictException {
    public SessionAlreadyActiveException() {
        super("Sessão de foco", "já existe uma sessão ativa");
    }
}
