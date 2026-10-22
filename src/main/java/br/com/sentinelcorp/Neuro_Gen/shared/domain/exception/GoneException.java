package br.com.sentinelcorp.Neuro_Gen.shared.domain.exception;

import org.springframework.http.HttpStatus;

/** HTTP 410 — recurso existia mas foi removido/expirou (ex.: token de convite expirado). */
public class GoneException extends DomainException {

    public GoneException(String resource, String detail) {
        super("GONE", resource + ": " + detail, HttpStatus.GONE);
    }
}
