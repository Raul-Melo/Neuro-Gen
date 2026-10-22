package br.com.sentinelcorp.Neuro_Gen.shared.domain.exception;

import org.springframework.http.HttpStatus;

/** HTTP 422 — requisição bem formada, mas semanticamente inválida para o estado atual. */
public class UnprocessableEntityException extends DomainException {

    public UnprocessableEntityException(String code, String message) {
        super(code, message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
