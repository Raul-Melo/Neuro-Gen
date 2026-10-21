package br.com.sentinelcorp.Neuro_Gen.emotional.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Check-in de humor (tabela {@code ng_sensitive.ng_mood_checkins}). {@code notesEnc} é cifrado. */
@Entity
@Table(name = "ng_mood_checkins", schema = "ng_sensitive")
@Getter
@Setter
@NoArgsConstructor
public class MoodCheckInEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "mood_score", nullable = false)
    private short moodScore;

    @Column(name = "energy_level", nullable = false)
    private short energyLevel;

    @Column(name = "notes_enc")
    private byte[] notesEnc;

    @Column(name = "checkin_date", nullable = false)
    private LocalDate checkinDate;

    @Column(name = "checkin_time", nullable = false)
    private LocalTime checkinTime;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
