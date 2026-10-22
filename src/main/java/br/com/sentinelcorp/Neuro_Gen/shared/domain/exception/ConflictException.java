package br.com.sentinelcorp.Neuro_Gen.shared.domain.exception;

import org.springframework.http.HttpStatus;

/** HTTP 409 — estado inconsistente ou recurso duplicado. */
public class ConflictException extends DomainException {

    public ConflictException(String resource, String detail) {
        super("CONFLICT", resource + ": " + detail, HttpStatus.CONFLICT);
    }
}
