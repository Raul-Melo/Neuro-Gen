package br.com.sentinelcorp.Neuro_Gen.agenda.application;

import br.com.sentinelcorp.Neuro_Gen.agenda.application.command.AgendaCommands.CreateEventCommand;
import br.com.sentinelcorp.Neuro_Gen.agenda.application.command.AgendaCommands.ReminderInput;
import br.com.sentinelcorp.Neuro_Gen.agenda.application.command.AgendaCommands.RescheduleEventCommand;
import br.com.sentinelcorp.Neuro_Gen.agenda.application.result.EventResult;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.exception.EventNotFoundException;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.CategoryId;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.Event;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.EventId;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.EventType;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.RRule;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.RecurrenceEditScope;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.Reminder;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.TimeRange;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.port.EventRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ForbiddenAccessException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** Use case de CRUD e reagendamento de eventos (RF-02). */
@Service
public class EventUseCase {

    private final EventRepository events;

    public EventUseCase(EventRepository events) {
        this.events = events;
    }

    @Transactional
    public EventResult create(CreateEventCommand cmd) {
        List<Reminder> reminders = toReminders(cmd.reminders());
        Event event = Event.create(
                UserId.of(cmd.ownerId()), cmd.title(), cmd.description(),
                new TimeRange(cmd.startAt(), cmd.endAt()),
                cmd.type() == null ? null : EventType.valueOf(cmd.type()),
                cmd.rrule() == null || cmd.rrule().isBlank() ? null : new RRule(cmd.rrule()),
                CategoryId.of(cmd.categoryId()), cmd.sharedWithSupport(), reminders);
        return EventResult.from(events.save(event));
    }

    @Transactional
    public EventResult reschedule(RescheduleEventCommand cmd) {
        Event event = load(cmd.eventId(), cmd.ownerId());
        event.reschedule(new TimeRange(cmd.startAt(), cmd.endAt()),
                RecurrenceEditScope.valueOf(cmd.scope()));
        return EventResult.from(events.save(event));
    }

    @Transactional
    public void cancel(String eventId, String ownerId, String scope) {
        Event event = load(eventId, ownerId);
        event.cancel(RecurrenceEditScope.valueOf(scope));
        events.save(event);
    }

    @Transactional
    public void delete(String eventId, String ownerId) {
        load(eventId, ownerId);
        events.delete(EventId.of(eventId));
    }

    @Transactional(readOnly = true)
    public List<EventResult> listByPeriod(String ownerId, Instant from, Instant to) {
        return events.findByOwnerAndPeriod(UserId.of(ownerId), from, to)
                .stream().map(EventResult::from).toList();
    }

    private Event load(String eventId, String ownerId) {
        Event event = events.findById(EventId.of(eventId))
                .orElseThrow(() -> new EventNotFoundException(eventId));
        if (!event.ownerId().equals(UserId.of(ownerId))) {
            throw new ForbiddenAccessException("Evento", "não pertence ao usuário");
        }
        return event;
    }

    private static List<Reminder> toReminders(List<ReminderInput> inputs) {
        if (inputs == null) {
            return List.of();
        }
        return inputs.stream()
                .map(r -> Reminder.create(r.minutesBefore(), r.persistent(),
                        r.repeatIntervalMinutes(), r.maxRepetitions()))
                .toList();
    }
}
