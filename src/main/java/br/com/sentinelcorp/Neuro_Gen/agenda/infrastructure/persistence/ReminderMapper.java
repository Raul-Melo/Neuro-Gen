package br.com.sentinelcorp.Neuro_Gen.agenda.infrastructure.persistence;

import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.Reminder;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.ReminderStatus;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.ReminderTrigger;

import java.time.Instant;
import java.util.UUID;

/** Conversão entre {@link Reminder} (domínio) e {@link ReminderEntity} (persistência). */
final class ReminderMapper {

    private ReminderMapper() {
    }

    static ReminderEntity toEntity(Reminder r, UUID eventId, UUID taskId) {
        ReminderEntity e = new ReminderEntity();
        e.setId(r.id());
        e.setEventId(eventId);
        e.setTaskId(taskId);
        e.setMinutesBefore(r.trigger().minutesBefore());
        e.setPersistent(r.persistent());
        e.setRepeatIntervalMin(r.persistent() ? r.repeatIntervalMinutes() : null);
        e.setMaxRepetitions(r.persistent() ? r.maxRepetitions() : null);
        e.setCurrentRepetitions(r.currentRepetitions());
        e.setStatus(r.status().name());
        e.setNextFireAt(r.nextFireAt());
        e.setConfirmationAt(r.confirmedAt());
        e.setCreatedAt(Instant.now());
        return e;
    }

    static Reminder toDomain(ReminderEntity e) {
        return new Reminder(
                e.getId(),
                new ReminderTrigger(e.getMinutesBefore()),
                e.isPersistent(),
                e.getRepeatIntervalMin() == null ? 0 : e.getRepeatIntervalMin(),
                e.getMaxRepetitions() == null ? 0 : e.getMaxRepetitions(),
                e.getCurrentRepetitions(),
                ReminderStatus.valueOf(e.getStatus()),
                e.getNextFireAt(),
                e.getConfirmationAt());
    }
}
