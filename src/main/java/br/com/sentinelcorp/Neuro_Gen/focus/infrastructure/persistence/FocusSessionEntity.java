package br.com.sentinelcorp.Neuro_Gen.focus.infrastructure.persistence;

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
@Table(name = "ng_focus_sessions", schema = "ng_core")
@Getter
@Setter
@NoArgsConstructor
public class FocusSessionEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "task_id")
    private UUID taskId;

    @Column(name = "planned_minutes", nullable = false)
    private int plannedMinutes;

    @Column(name = "break_minutes", nullable = false)
    private int breakMinutes;

    @Column(nullable = false)
    private String status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "effective_seconds", nullable = false)
    private long effectiveSeconds;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
