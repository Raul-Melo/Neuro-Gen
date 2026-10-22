package br.com.sentinelcorp.Neuro_Gen.shared.api;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.NeuroGenException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    // ------------------------------------------------------------------------------------------
    // Erros de requisição do cliente traduzidos pelo Spring MVC. Precisam de handler próprio:
    // sem isso, o handler genérico de Exception os transformaria em HTTP 500.
    // ------------------------------------------------------------------------------------------

    /** Corpo ausente, JSON malformado ou valor incompatível com o tipo do campo. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                              HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "Corpo da requisição ausente ou com JSON inválido", req, Map.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex,
                                                                HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER",
                "Parâmetro obrigatório ausente: " + ex.getParameterName(), req,
                Map.of(ex.getParameterName(), "Parâmetro obrigatório"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "Valor inválido para o parâmetro: " + ex.getName(), req,
                Map.of(ex.getName(), "Formato inválido"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                  HttpServletRequest req) {
        Set<HttpMethod> supported = ex.getSupportedHttpMethods();
        ErrorResponse body = ErrorResponse.of(HttpStatus.METHOD_NOT_ALLOWED.value(),
                HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(), "METHOD_NOT_ALLOWED",
                "Método " + ex.getMethod() + " não suportado neste endpoint", req.getRequestURI(), Map.of());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .allow(supported == null ? new HttpMethod[0] : supported.toArray(new HttpMethod[0]))
                .body(body);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex,
                                                         HttpServletRequest req) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "Content-Type não suportado; use application/json", req, Map.of());
    }

    /** Rota inexistente: NoResourceFoundException (Spring 6.1+) ou NoHandlerFoundException. */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> handleNoRoute(Exception ex, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Recurso não encontrado", req, Map.of());
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

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message,
                                                       HttpServletRequest req, Map<String, String> fields) {
        return ResponseEntity.status(status).body(ErrorResponse.of(
                status.value(), status.getReasonPhrase(), code, message, req.getRequestURI(), fields));
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
