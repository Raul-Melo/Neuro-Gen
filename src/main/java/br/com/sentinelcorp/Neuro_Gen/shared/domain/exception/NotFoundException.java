package br.com.sentinelcorp.Neuro_Gen.shared.domain.exception;

import org.springframework.http.HttpStatus;

/** HTTP 404 — recurso não existe (ou não existe para este usuário, evitando enumeração). */
public class NotFoundException extends DomainException {

    public NotFoundException(String resource, String id) {
        super("RESOURCE_NOT_FOUND", resource + " não encontrado: " + id, HttpStatus.NOT_FOUND);
    }
}
