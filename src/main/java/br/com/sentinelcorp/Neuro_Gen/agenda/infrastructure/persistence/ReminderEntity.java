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

/** Lembrete vinculado a um evento XOR tarefa (tabela {@code ng_core.ng_reminders}). */
@Entity
@Table(name = "ng_reminders", schema = "ng_core")
@Getter
@Setter
@NoArgsConstructor
public class ReminderEntity {

    @Id
    private UUID id;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "task_id")
    private UUID taskId;

    @Column(name = "minutes_before", nullable = false)
    private int minutesBefore;

    @Column(nullable = false)
    private boolean persistent;

    @Column(name = "repeat_interval_min")
    private Integer repeatIntervalMin;

    @Column(name = "max_repetitions")
    private Integer maxRepetitions;

    @Column(name = "current_repetitions", nullable = false)
    private int currentRepetitions;

    @Column(nullable = false)
    private String status;

    @Column(name = "next_fire_at")
    private Instant nextFireAt;

    @Column(name = "confirmation_at")
    private Instant confirmationAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
