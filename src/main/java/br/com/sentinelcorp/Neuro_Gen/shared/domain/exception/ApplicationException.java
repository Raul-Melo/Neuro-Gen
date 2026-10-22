package br.com.sentinelcorp.Neuro_Gen.shared.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Falhas na orquestração dos use cases (estados impossíveis, invariantes de aplicação).
 * Mapeadas para 500 — a mensagem nunca deve vazar detalhe interno ao usuário.
 */
public abstract class ApplicationException extends NeuroGenException {

    protected ApplicationException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
