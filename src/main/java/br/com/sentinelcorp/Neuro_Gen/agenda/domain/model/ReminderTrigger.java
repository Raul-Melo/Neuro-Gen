package br.com.sentinelcorp.Neuro_Gen.agenda.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/** Offset relativo (em minutos) antes do evento/tarefa para disparo do lembrete. */
public record ReminderTrigger(int minutesBefore) {

    public ReminderTrigger {
        if (minutesBefore < 0) {
            throw new ValidationException(Map.of("minutesBefore", "não pode ser negativo"));
        }
    }

    public Instant resolveFor(Instant baseTime) {
        return baseTime.minus(minutesBefore, ChronoUnit.MINUTES);
    }
}
