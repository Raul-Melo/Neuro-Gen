package br.com.sentinelcorp.Neuro_Gen.agenda.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

public record RescheduleEventRequest(
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        @Pattern(regexp = "THIS_OCCURRENCE|THIS_AND_FUTURE|ALL_OCCURRENCES") String scope) {

    public String scopeOrDefault() {
        return scope == null ? "THIS_OCCURRENCE" : scope;
    }
}
