package br.com.sentinelcorp.Neuro_Gen.shared.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Fachada de auditoria para a camada de aplicação.
 * Grava em ng_audit.ng_system_logs de forma assíncrona para não bloquear a request.
 * Em caso de falha ao persistir, a operação original NÃO é revertida — a auditoria é
 * best-effort em nível de aplicação; a auditoria de banco (triggers) é síncrona.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final SystemLogPort port;

    public AuditService(SystemLogPort port) {
        this.port = port;
    }

    @Async
    public void logAction(String action, UUID userId, String resourceType, String resourceId,
                          Map<String, Object> metadata) {
        try {
            port.append(SystemLogEntry.builder(SystemLogEntry.Level.INFO,
                            "Ação de domínio: " + action)
                    .action(action)
                    .userId(userId)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .metadata(metadata)
                    .build());
        } catch (Exception ex) {
            log.warn("Falha ao persistir log de auditoria (ação={}, userId={}): {}", action, userId, ex.getMessage());
        }
    }

    @Async
    public void logHttp(String method, String path, int status, long durationMs,
                        UUID userId, String ip, String userAgent) {
        try {
            port.append(SystemLogEntry.builder(SystemLogEntry.Level.INFO,
                            method + " " + path + " → " + status)
                    .httpMethod(method)
                    .requestPath(path)
                    .httpStatus(status)
                    .durationMs((int) durationMs)
                    .userId(userId)
                    .ipAddress(ip)
                    .userAgent(userAgent)
                    .build());
        } catch (Exception ex) {
            log.warn("Falha ao persistir log HTTP: {}", ex.getMessage());
        }
    }

    @Async
    public void logError(String message, Throwable cause, UUID userId, String requestPath) {
        try {
            String stack = cause != null ? stackToString(cause) : null;
            port.append(SystemLogEntry.builder(SystemLogEntry.Level.ERROR, message)
                    .stackTrace(stack)
                    .userId(userId)
                    .requestPath(requestPath)
                    .build());
        } catch (Exception ex) {
            log.warn("Falha ao persistir log de erro: {}", ex.getMessage());
        }
    }

    @Async
    public void logSecurity(String action, UUID userId, String ip, boolean success,
                            Map<String, Object> metadata) {
        SystemLogEntry.Level level = success ? SystemLogEntry.Level.INFO : SystemLogEntry.Level.WARN;
        try {
            port.append(SystemLogEntry.builder(level,
                            (success ? "[SEC-OK] " : "[SEC-FAIL] ") + action)
                    .action(action)
                    .userId(userId)
                    .ipAddress(ip)
                    .metadata(metadata)
                    .build());
        } catch (Exception ex) {
            log.warn("Falha ao persistir log de segurança: {}", ex.getMessage());
        }
    }

    private static String stackToString(Throwable t) {
        var sb = new StringBuilder(t.toString());
        for (StackTraceElement e : t.getStackTrace()) {
            sb.append("\n\tat ").append(e);
        }
        if (t.getCause() != null) {
            sb.append("\nCaused by: ").append(stackToString(t.getCause()));
        }
        return sb.length() > 8000 ? sb.substring(0, 8000) + "…" : sb.toString();
    }
}
