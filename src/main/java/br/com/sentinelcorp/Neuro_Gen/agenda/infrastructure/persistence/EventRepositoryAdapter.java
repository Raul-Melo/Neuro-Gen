package br.com.sentinelcorp.Neuro_Gen.agenda.infrastructure.persistence;

import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.CategoryId;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.Event;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.EventId;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.EventType;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.RRule;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.Reminder;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.TimeRange;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.port.EventRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.AuditMetadata;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class EventRepositoryAdapter implements EventRepository {

    private final EventJpaRepository eventJpa;
    private final ReminderJpaRepository reminderJpa;

    public EventRepositoryAdapter(EventJpaRepository eventJpa, ReminderJpaRepository reminderJpa) {
        this.eventJpa = eventJpa;
        this.reminderJpa = reminderJpa;
    }

    @Override
    public Event save(Event event) {
        EventEntity e = eventJpa.findById(event.id().value()).orElseGet(EventEntity::new);
        e.setId(event.id().value());
        e.setOwnerId(event.ownerId().value());
        e.setTitle(event.title());
        e.setDescription(event.description());
        e.setStartAt(event.timeRange().start());
        e.setEndAt(event.timeRange().end());
        e.setEventType(event.type().name());
        e.setRrule(event.recurrenceRule() == null ? null : event.recurrenceRule().rfc5545String());
        e.setCategoryId(event.categoryId() == null ? null : event.categoryId().value());
        e.setSharedWithSupport(event.sharedWithSupport());
        if (e.getCreatedAt() == null) {
            e.setCreatedAt(event.audit().createdAt());
        }
        e.setUpdatedAt(event.audit().updatedAt());
        e.setCancelledAt(event.cancelledAt());
        eventJpa.save(e);

        reminderJpa.deleteByEventId(event.id().value());
        for (Reminder r : event.reminders()) {
            reminderJpa.save(ReminderMapper.toEntity(r, event.id().value(), null));
        }
        return event;
    }

    @Override
    public Optional<Event> findById(EventId id) {
        return eventJpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Event> findByOwnerAndPeriod(UserId owner, Instant from, Instant to) {
        return eventJpa.findByOwnerIdAndStartAtBetween(owner.value(), from, to)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(EventId id) {
        reminderJpa.deleteByEventId(id.value());
        eventJpa.deleteById(id.value());
    }

    private Event toDomain(EventEntity e) {
        List<Reminder> reminders = reminderJpa.findByEventId(e.getId())
                .stream().map(ReminderMapper::toDomain).toList();
        return Event.reconstitute(
                new EventId(e.getId()),
                new UserId(e.getOwnerId()),
                e.getTitle(),
                e.getDescription(),
                new TimeRange(e.getStartAt(), e.getEndAt()),
                EventType.valueOf(e.getEventType()),
                e.getRrule() == null ? null : new RRule(e.getRrule()),
                reminders,
                e.getCategoryId() == null ? null : new CategoryId(e.getCategoryId()),
                e.isSharedWithSupport(),
                e.getCancelledAt(),
                new AuditMetadata(e.getCreatedAt(), e.getUpdatedAt(), e.getOwnerId().toString()));
    }
}
