package br.com.sentinelcorp.Neuro_Gen.agenda.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/** Intervalo de tempo validado (início &lt; fim). */
public record TimeRange(Instant start, Instant end) {

    public TimeRange {
        if (start == null || end == null) {
            throw new ValidationException(Map.of("timeRange", "início e fim são obrigatórios"));
        }
        if (!start.isBefore(end)) {
            throw new ValidationException(Map.of("timeRange", "o início deve ser anterior ao fim"));
        }
    }

    public Duration duration() {
        return Duration.between(start, end);
    }

    public boolean overlaps(TimeRange other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    public boolean isInFuture() {
        return start.isAfter(Instant.now());
    }
}
