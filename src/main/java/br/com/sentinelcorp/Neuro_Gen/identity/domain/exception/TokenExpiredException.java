package br.com.sentinelcorp.Neuro_Gen.identity.domain.exception;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.DomainException;
import org.springframework.http.HttpStatus;

/** HTTP 401 — token de verificação, reset ou refresh expirado/inválido. */
public class TokenExpiredException extends DomainException {
    public TokenExpiredException(String kind) {
        super("TOKEN_EXPIRED", "Token de " + kind + " expirado ou inválido", HttpStatus.UNAUTHORIZED);
    }
}
