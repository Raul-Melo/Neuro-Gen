package br.com.sentinelcorp.Neuro_Gen.focus.domain.model;

import java.time.Duration;
import java.time.Instant;

/** Value Object imutável de uma pausa. Criado ao pausar e completado (resumedAt) ao retomar. */
public record PauseRecord(Instant pausedAt, Instant resumedAt) {

    public static PauseRecord start(Instant pausedAt) {
        return new PauseRecord(pausedAt, null);
    }

    public PauseRecord end(Instant resumedAt) {
        return new PauseRecord(pausedAt, resumedAt);
    }

    public Duration pauseDuration() {
        return Duration.between(pausedAt, resumedAt == null ? Instant.now() : resumedAt);
    }

    public boolean isActive() {
        return resumedAt == null;
    }
}
