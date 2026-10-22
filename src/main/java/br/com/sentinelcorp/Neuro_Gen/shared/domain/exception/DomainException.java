package br.com.sentinelcorp.Neuro_Gen.shared.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Violações das regras de negócio do domínio. Mapeadas para 4xx (400, 403, 404, 409, 410, 422).
 */
public abstract class DomainException extends NeuroGenException {

    protected DomainException(String errorCode, String message, HttpStatus httpStatus) {
        super(errorCode, message, httpStatus);
    }
}
