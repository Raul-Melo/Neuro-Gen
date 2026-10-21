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
import java.util.UUID;

/** Diário emocional (tabela {@code ng_sensitive.ng_journal_entries}). {@code contentEnc} é cifrado. */
@Entity
@Table(name = "ng_journal_entries", schema = "ng_sensitive")
@Getter
@Setter
@NoArgsConstructor
public class JournalEntryEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "content_enc", nullable = false)
    private byte[] contentEnc;

    @Column(name = "related_checkin_id")
    private UUID relatedCheckinId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
