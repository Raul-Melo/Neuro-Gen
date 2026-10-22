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
@Table(name = "ng_focus_pauses", schema = "ng_core")
@Getter
@Setter
@NoArgsConstructor
public class FocusPauseEntity {

    @Id
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "paused_at", nullable = false)
    private Instant pausedAt;

    @Column(name = "resumed_at")
    private Instant resumedAt;
}
