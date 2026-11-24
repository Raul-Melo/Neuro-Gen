package br.com.sentinelcorp.Neuro_Gen.shared.audit.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "ng_system_logs", schema = "ng_audit")
class SystemLogEntity {

    @Id
    private UUID id;

    @Column(name = "logged_at", nullable = false)
    private Instant loggedAt;

    @Column(name = "level", length = 10, nullable = false)
    private String level;

    @Column(name = "logger_name")
    private String loggerName;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "span_id", length = 32)
    private String spanId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "request_path")
    private String requestPath;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "http_status")
    private Short httpStatus;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "action", length = 80)
    private String action;

    @Column(name = "resource_type", length = 80)
    private String resourceType;

    @Column(name = "resource_id")
    private String resourceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    protected SystemLogEntity() {}

    SystemLogEntity(UUID id, Instant loggedAt, String level, String loggerName,
                    String message, String stackTrace, String traceId, String spanId,
                    UUID userId, String requestPath, String httpMethod, Short httpStatus,
                    Integer durationMs, String ipAddress, String userAgent,
                    String action, String resourceType, String resourceId,
                    Map<String, Object> metadata) {
        this.id           = id;
        this.loggedAt     = loggedAt;
        this.level        = level;
        this.loggerName   = loggerName;
        this.message      = message;
        this.stackTrace   = stackTrace;
        this.traceId      = traceId;
        this.spanId       = spanId;
        this.userId       = userId;
        this.requestPath  = requestPath;
        this.httpMethod   = httpMethod;
        this.httpStatus   = httpStatus;
        this.durationMs   = durationMs;
        this.ipAddress    = ipAddress;
        this.userAgent    = userAgent;
        this.action       = action;
        this.resourceType = resourceType;
        this.resourceId   = resourceId;
        this.metadata     = metadata;
    }
}
