package br.com.sentinelcorp.Neuro_Gen.agenda.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.AggregateRoot;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.AuditMetadata;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Aggregate Root para compromissos com hora fixa (Event). */
public class Event extends AggregateRoot {

    private final EventId id;
    private final UserId ownerId;
    private String title;
    private String description;
    private TimeRange timeRange;
    private EventType type;
    private RRule recurrenceRule;          // nullable
    private final List<Reminder> reminders;
    private CategoryId categoryId;          // nullable
    private boolean sharedWithSupport;
    private Instant cancelledAt;
    private AuditMetadata audit;

    private Event(EventId id, UserId ownerId, String title, String description, TimeRange timeRange,
                  EventType type, RRule recurrenceRule, List<Reminder> reminders, CategoryId categoryId,
                  boolean sharedWithSupport, Instant cancelledAt, AuditMetadata audit) {
        this.id = id;
        this.ownerId = ownerId;
        this.title = title;
        this.description = description;
        this.timeRange = timeRange;
        this.type = type;
        this.recurrenceRule = recurrenceRule;
        this.reminders = new ArrayList<>(reminders);
        this.categoryId = categoryId;
        this.sharedWithSupport = sharedWithSupport;
        this.cancelledAt = cancelledAt;
        this.audit = audit;
    }

    public static Event create(UserId ownerId, String title, String description, TimeRange timeRange,
                               EventType type, RRule recurrenceRule, CategoryId categoryId,
                               boolean sharedWithSupport, List<Reminder> reminders) {
        Event event = new Event(EventId.generate(), ownerId, title, description, timeRange,
                type == null ? EventType.APPOINTMENT : type, recurrenceRule, reminders, categoryId,
                sharedWithSupport, null, AuditMetadata.createNow(ownerId.asString()));
        event.reminders.forEach(r -> r.schedule(timeRange.start()));
        return event;
    }

    public static Event reconstitute(EventId id, UserId ownerId, String title, String description,
                                     TimeRange timeRange, EventType type, RRule recurrenceRule,
                                     List<Reminder> reminders, CategoryId categoryId,
                                     boolean sharedWithSupport, Instant cancelledAt, AuditMetadata audit) {
        return new Event(id, ownerId, title, description, timeRange, type, recurrenceRule, reminders,
                categoryId, sharedWithSupport, cancelledAt, audit);
    }

    public void reschedule(TimeRange newRange, RecurrenceEditScope scope) {
        // scope registra a intenção do usuário (RF-02); séries completas seriam divididas em evolução.
        this.timeRange = newRange;
        this.reminders.forEach(r -> r.schedule(newRange.start()));
        this.audit = audit.touch(ownerId.asString());
    }

    public void cancel(RecurrenceEditScope scope) {
        this.cancelledAt = Instant.now();
        this.audit = audit.touch(ownerId.asString());
    }

    public void addReminder(Reminder reminder) {
        reminder.schedule(timeRange.start());
        this.reminders.add(reminder);
        this.audit = audit.touch(ownerId.asString());
    }

    public void removeReminder(UUID reminderId) {
        this.reminders.removeIf(r -> r.id().equals(reminderId));
        this.audit = audit.touch(ownerId.asString());
    }

    public boolean isCancelled() {
        return cancelledAt != null;
    }

    public EventId id() { return id; }
    public UserId ownerId() { return ownerId; }
    public String title() { return title; }
    public String description() { return description; }
    public TimeRange timeRange() { return timeRange; }
    public EventType type() { return type; }
    public RRule recurrenceRule() { return recurrenceRule; }
    public List<Reminder> reminders() { return List.copyOf(reminders); }
    public CategoryId categoryId() { return categoryId; }
    public boolean sharedWithSupport() { return sharedWithSupport; }
    public Instant cancelledAt() { return cancelledAt; }
    public AuditMetadata audit() { return audit; }
}
