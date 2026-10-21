package br.com.sentinelcorp.Neuro_Gen.agenda.application.result;

import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.Event;

import java.util.List;

public record EventResult(
        String id,
        String title,
        String description,
        String startAt,
        String endAt,
        String eventType,
        String rrule,
        String categoryId,
        boolean sharedWithSupport,
        boolean cancelled,
        List<ReminderResult> reminders) {

    public static EventResult from(Event e) {
        return new EventResult(
                e.id().asString(),
                e.title(),
                e.description(),
                e.timeRange().start().toString(),
                e.timeRange().end().toString(),
                e.type().name(),
                e.recurrenceRule() == null ? null : e.recurrenceRule().rfc5545String(),
                e.categoryId() == null ? null : e.categoryId().asString(),
                e.sharedWithSupport(),
                e.isCancelled(),
                e.reminders().stream().map(ReminderResult::from).toList());
    }
}
