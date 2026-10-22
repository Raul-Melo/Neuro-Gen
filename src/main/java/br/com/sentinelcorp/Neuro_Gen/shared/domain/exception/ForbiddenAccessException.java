package br.com.sentinelcorp.Neuro_Gen.shared.domain.exception;

import org.springframework.http.HttpStatus;

/** HTTP 403 — autenticado, porém sem permissão (ex.: Apoio acessa módulo não liberado). */
public class ForbiddenAccessException extends DomainException {

    public ForbiddenAccessException(String module, String reason) {
        super("FORBIDDEN_ACCESS", "Acesso negado a " + module + ": " + reason, HttpStatus.FORBIDDEN);
    }
}
