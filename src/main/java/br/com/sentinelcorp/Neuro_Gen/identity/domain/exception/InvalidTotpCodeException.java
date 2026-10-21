package br.com.sentinelcorp.Neuro_Gen.identity.domain.exception;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.DomainException;
import org.springframework.http.HttpStatus;

/** HTTP 401 — código TOTP inválido ou fora da janela de 30s. */
public class InvalidTotpCodeException extends DomainException {
    public InvalidTotpCodeException() {
        super("INVALID_TOTP_CODE", "Código 2FA inválido", HttpStatus.UNAUTHORIZED);
    }
}
