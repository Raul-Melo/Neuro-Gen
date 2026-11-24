package br.com.sentinelcorp.Neuro_Gen.shared.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Evento de log imutável gravado em ng_audit.ng_system_logs.
 * Construído via builder — nunca por new direto.
 */
public final class SystemLogEntry {

    public enum Level { TRACE, DEBUG, INFO, WARN, ERROR, FATAL }

    private final UUID          id;
    private final Instant       loggedAt;
    private final Level         level;
    private final String        loggerName;
    private final String        message;
    private final String        stackTrace;
    private final String        traceId;
    private final String        spanId;
    private final UUID          userId;
    private final String        requestPath;
    private final String        httpMethod;
    private final Integer       httpStatus;
    private final Integer       durationMs;
    private final String        ipAddress;
    private final String        userAgent;
    private final String        action;
    private final String        resourceType;
    private final String        resourceId;
    private final Map<String, Object> metadata;

    private SystemLogEntry(Builder b) {
        this.id           = UUID.randomUUID();
        this.loggedAt     = Instant.now();
        this.level        = b.level;
        this.loggerName   = b.loggerName;
        this.message      = b.message;
        this.stackTrace   = b.stackTrace;
        this.traceId      = b.traceId;
        this.spanId       = b.spanId;
        this.userId       = b.userId;
        this.requestPath  = b.requestPath;
        this.httpMethod   = b.httpMethod;
        this.httpStatus   = b.httpStatus;
        this.durationMs   = b.durationMs;
        this.ipAddress    = b.ipAddress;
        this.userAgent    = b.userAgent;
        this.action       = b.action;
        this.resourceType = b.resourceType;
        this.resourceId   = b.resourceId;
        this.metadata     = b.metadata == null ? Map.of() : Map.copyOf(b.metadata);
    }

    public static Builder builder(Level level, String message) {
        return new Builder(level, message);
    }

    public UUID getId()            { return id; }
    public Instant getLoggedAt()   { return loggedAt; }
    public Level getLevel()        { return level; }
    public String getLoggerName()  { return loggerName; }
    public String getMessage()     { return message; }
    public String getStackTrace()  { return stackTrace; }
    public String getTraceId()     { return traceId; }
    public String getSpanId()      { return spanId; }
    public UUID getUserId()        { return userId; }
    public String getRequestPath() { return requestPath; }
    public String getHttpMethod()  { return httpMethod; }
    public Integer getHttpStatus() { return httpStatus; }
    public Integer getDurationMs() { return durationMs; }
    public String getIpAddress()   { return ipAddress; }
    public String getUserAgent()   { return userAgent; }
    public String getAction()      { return action; }
    public String getResourceType(){ return resourceType; }
    public String getResourceId()  { return resourceId; }
    public Map<String, Object> getMetadata() { return metadata; }

    public static final class Builder {
        private final Level  level;
        private final String message;
        private String  loggerName;
        private String  stackTrace;
        private String  traceId;
        private String  spanId;
        private UUID    userId;
        private String  requestPath;
        private String  httpMethod;
        private Integer httpStatus;
        private Integer durationMs;
        private String  ipAddress;
        private String  userAgent;
        private String  action;
        private String  resourceType;
        private String  resourceId;
        private Map<String, Object> metadata;

        private Builder(Level level, String message) {
            this.level   = level;
            this.message = message;
        }

        public Builder loggerName(String v)  { this.loggerName   = v; return this; }
        public Builder stackTrace(String v)  { this.stackTrace   = v; return this; }
        public Builder traceId(String v)     { this.traceId      = v; return this; }
        public Builder spanId(String v)      { this.spanId       = v; return this; }
        public Builder userId(UUID v)        { this.userId       = v; return this; }
        public Builder requestPath(String v) { this.requestPath  = v; return this; }
        public Builder httpMethod(String v)  { this.httpMethod   = v; return this; }
        public Builder httpStatus(Integer v) { this.httpStatus   = v; return this; }
        public Builder durationMs(Integer v) { this.durationMs   = v; return this; }
        public Builder ipAddress(String v)   { this.ipAddress    = v; return this; }
        public Builder userAgent(String v)   { this.userAgent    = v; return this; }
        public Builder action(String v)      { this.action       = v; return this; }
        public Builder resourceType(String v){ this.resourceType = v; return this; }
        public Builder resourceId(String v)  { this.resourceId   = v; return this; }
        public Builder metadata(Map<String, Object> v) { this.metadata = v; return this; }

        public SystemLogEntry build() { return new SystemLogEntry(this); }
    }
}
