package br.com.sentinelcorp.Neuro_Gen.shared.domain.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/** HTTP 400 — dados de entrada inválidos. Carrega os erros por campo. */
public class ValidationException extends DomainException {

    private final transient Map<String, String> fieldErrors;

    public ValidationException(Map<String, String> fieldErrors) {
        super("VALIDATION_ERROR", "Dados de entrada inválidos", HttpStatus.BAD_REQUEST);
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
