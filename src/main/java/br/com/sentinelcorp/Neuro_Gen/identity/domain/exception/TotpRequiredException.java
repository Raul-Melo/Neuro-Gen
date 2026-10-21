package br.com.sentinelcorp.Neuro_Gen.identity.domain.exception;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.DomainException;
import org.springframework.http.HttpStatus;

/** HTTP 401 — 2FA habilitado: o passo TOTP é obrigatório para concluir o login. */
public class TotpRequiredException extends DomainException {
    public TotpRequiredException() {
        super("TOTP_REQUIRED", "Código 2FA (TOTP) é obrigatório para esta conta", HttpStatus.UNAUTHORIZED);
    }
}
