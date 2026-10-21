package br.com.sentinelcorp.Neuro_Gen.identity.domain.exception;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.DomainException;
import org.springframework.http.HttpStatus;

/** HTTP 401 — e-mail ou senha incorretos. Mensagem genérica: não revela qual campo está errado. */
public class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "Credenciais inválidas", HttpStatus.UNAUTHORIZED);
    }
}
