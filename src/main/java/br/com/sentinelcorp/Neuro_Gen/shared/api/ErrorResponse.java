package br.com.sentinelcorp.Neuro_Gen.shared.api;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.NeuroGenException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.Map;

/**
 * Corpo JSON padronizado de toda resposta de erro da API (inspirado na RFC 7807 — Problem Details).
 * Produzido exclusivamente pelo {@link GlobalExceptionHandler} — jamais expõe stack trace.
 */
public record ErrorResponse(
        String timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        Map<String, String> fieldErrors) {

    public static ErrorResponse of(NeuroGenException ex, HttpServletRequest req) {
        Map<String, String> fields =
                (ex instanceof ValidationException ve) ? ve.getFieldErrors() : Map.of();
        return new ErrorResponse(
                Instant.now().toString(),
                ex.getHttpStatus().value(),
                ex.getHttpStatus().getReasonPhrase(),
                ex.getErrorCode(),
                ex.getMessage(),
                req.getRequestURI(),
                fields);
    }

    public static ErrorResponse of(int status, String error, String code, String message,
                                   String path, Map<String, String> fieldErrors) {
        return new ErrorResponse(Instant.now().toString(), status, error, code, message, path,
                fieldErrors == null ? Map.of() : fieldErrors);
    }
}
