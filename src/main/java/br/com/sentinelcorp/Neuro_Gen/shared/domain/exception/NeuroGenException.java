package br.com.sentinelcorp.Neuro_Gen.shared.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Raiz da hierarquia de exceções do sistema. Carrega um {@code errorCode} estável (consumível por
 * clientes) e o {@link HttpStatus} correspondente, usados pelo {@code GlobalExceptionHandler}
 * para produzir respostas padronizadas (RFC 7807) sem vazar stack trace.
 */
public abstract class NeuroGenException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    protected NeuroGenException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
