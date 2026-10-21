package br.com.sentinelcorp.Neuro_Gen.agenda.application.result;

import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.Reminder;

public record ReminderResult(String id, int minutesBefore, boolean persistent, String status) {

    public static ReminderResult from(Reminder r) {
        return new ReminderResult(r.id().toString(), r.trigger().minutesBefore(),
                r.persistent(), r.status().name());
    }
}
