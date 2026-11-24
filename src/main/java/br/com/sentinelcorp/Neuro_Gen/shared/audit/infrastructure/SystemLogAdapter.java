package br.com.sentinelcorp.Neuro_Gen.shared.audit.infrastructure;

import br.com.sentinelcorp.Neuro_Gen.shared.audit.SystemLogEntry;
import br.com.sentinelcorp.Neuro_Gen.shared.audit.SystemLogPort;
import org.springframework.stereotype.Component;

@Component
class SystemLogAdapter implements SystemLogPort {

    private final SystemLogJpaRepository repo;

    SystemLogAdapter(SystemLogJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public void append(SystemLogEntry e) {
        repo.save(new SystemLogEntity(
                e.getId(),
                e.getLoggedAt(),
                e.getLevel().name(),
                e.getLoggerName(),
                e.getMessage(),
                e.getStackTrace(),
                e.getTraceId(),
                e.getSpanId(),
                e.getUserId(),
                e.getRequestPath(),
                e.getHttpMethod(),
                e.getHttpStatus() == null ? null : e.getHttpStatus().shortValue(),
                e.getDurationMs(),
                e.getIpAddress(),
                e.getHttpStatus() == null ? e.getUserAgent() : e.getUserAgent(),
                e.getAction(),
                e.getResourceType(),
                e.getResourceId(),
                e.getMetadata()
        ));
    }
}
