package br.com.sentinelcorp.Neuro_Gen.agenda.application.command;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Commands imutáveis do módulo Agenda. */
public final class AgendaCommands {

    private AgendaCommands() {
    }

    public record ReminderInput(int minutesBefore, boolean persistent,
                                int repeatIntervalMinutes, int maxRepetitions) {
    }

    public record CreateEventCommand(String ownerId, String title, String description,
                                     Instant startAt, Instant endAt, String rrule, String type,
                                     String categoryId, boolean sharedWithSupport,
                                     List<ReminderInput> reminders) {
    }

    public record RescheduleEventCommand(String eventId, String ownerId, Instant startAt, Instant endAt,
                                         String scope) {
    }

    public record CreateTaskCommand(String ownerId, String title, String description, String priority,
                                    LocalDate dueDate, String categoryId, boolean sharedWithSupport,
                                    List<ReminderInput> reminders) {
    }
}
