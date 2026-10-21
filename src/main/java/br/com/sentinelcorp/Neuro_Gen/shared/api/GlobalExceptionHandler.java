package br.com.sentinelcorp.Neuro_Gen.shared.api;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.NeuroGenException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Intercepta toda exceção lançada em qualquer camada e a mapeia para {@link ErrorResponse} com o
 * status HTTP correto. Garante que nenhum stack trace ou detalhe interno vaze ao cliente (RNF-OBS.1).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Toda a hierarquia {@link NeuroGenException} já carrega code + status. */
    @ExceptionHandler(NeuroGenException.class)
    public ResponseEntity<ErrorResponse> handleNeuroGen(NeuroGenException ex, HttpServletRequest req) {
        if (ex.getHttpStatus().is5xxServerError()) {
            log.error("Erro de servidor [{}] em {}", ex.getErrorCode(), req.getRequestURI(), ex);
        } else {
            log.debug("Erro de negócio [{}] em {}: {}", ex.getErrorCode(), req.getRequestURI(), ex.getMessage());
        }
        return ResponseEntity.status(ex.getHttpStatus()).body(ErrorResponse.of(ex, req));
    }

    /** Bean Validation nos Request DTOs (@Valid). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException ex,
                                                              HttpServletRequest req) {
        Map<String, String> fields = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fields.put(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "VALIDATION_ERROR", "Dados de entrada inválidos", req.getRequestURI(), fields));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(), HttpStatus.FORBIDDEN.getReasonPhrase(),
                "FORBIDDEN_ACCESS", "Acesso negado", req.getRequestURI(), Map.of()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "UNAUTHENTICATED", "Não autenticado", req.getRequestURI(), Map.of()));
    }

    /** Qualquer exceção não prevista → 500 genérico, sem vazar detalhe. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado em {}", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "INTERNAL_ERROR", "Ocorreu um erro inesperado", req.getRequestURI(), Map.of()));
    }
}
