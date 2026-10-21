package br.com.sentinelcorp.Neuro_Gen.agenda.api.dto;

import br.com.sentinelcorp.Neuro_Gen.agenda.application.command.AgendaCommands.ReminderInput;
import jakarta.validation.constraints.Min;

public record ReminderRequest(
        @Min(0) int minutesBefore,
        boolean persistent,
        @Min(0) int repeatIntervalMinutes,
        @Min(0) int maxRepetitions) {

    public ReminderInput toInput() {
        return new ReminderInput(minutesBefore, persistent, repeatIntervalMinutes, maxRepetitions);
    }
}
