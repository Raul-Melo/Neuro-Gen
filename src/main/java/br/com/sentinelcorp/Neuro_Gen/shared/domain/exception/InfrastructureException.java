package br.com.sentinelcorp.Neuro_Gen.shared.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Falhas em banco, fila, cache ou serviços externos. Mapeadas para 502/503.
 */
public abstract class InfrastructureException extends NeuroGenException {

    protected InfrastructureException(String errorCode, String message, HttpStatus httpStatus) {
        super(errorCode, message, httpStatus);
    }
}
