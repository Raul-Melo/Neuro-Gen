package br.com.sentinelcorp.Neuro_Gen.agenda.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ng_events", schema = "ng_core")
@Getter
@Setter
@NoArgsConstructor
public class EventEntity {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "rrule")
    private String rrule;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "shared_with_support", nullable = false)
    private boolean sharedWithSupport;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;
}
