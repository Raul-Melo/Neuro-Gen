package br.com.sentinelcorp.Neuro_Gen.emotional.infrastructure.persistence;

import br.com.sentinelcorp.Neuro_Gen.emotional.domain.model.JournalEntry;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.model.JournalEntryId;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.model.MoodCheckInId;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.port.JournalEntryRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.AuditMetadata;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import br.com.sentinelcorp.Neuro_Gen.shared.security.AesGcmCipher;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Adaptador do diário emocional. {@code content} é integralmente cifrado em repouso. */
@Repository
public class JournalEntryRepositoryAdapter implements JournalEntryRepository {

    private final JournalEntryJpaRepository jpa;
    private final AesGcmCipher cipher;

    public JournalEntryRepositoryAdapter(JournalEntryJpaRepository jpa, AesGcmCipher cipher) {
        this.jpa = jpa;
        this.cipher = cipher;
    }

    @Override
    public JournalEntry save(JournalEntry entry) {
        JournalEntryEntity e = jpa.findById(entry.id().value()).orElseGet(JournalEntryEntity::new);
        e.setId(entry.id().value());
        e.setUserId(entry.userId().value());
        e.setContentEnc(cipher.encryptToBase64(entry.content()).getBytes(StandardCharsets.UTF_8));
        e.setRelatedCheckinId(entry.relatedCheckInId() == null ? null : entry.relatedCheckInId().value());
        e.setEntryDate(entry.entryDate());
        if (e.getCreatedAt() == null) {
            e.setCreatedAt(entry.audit().createdAt());
        }
        e.setUpdatedAt(entry.audit().updatedAt());
        jpa.save(e);
        return entry;
    }

    @Override
    public Optional<JournalEntry> findById(JournalEntryId id) {
        return jpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<JournalEntry> findByUserId(UserId userId) {
        return jpa.findByUserIdOrderByEntryDateDesc(userId.value()).stream().map(this::toDomain).toList();
    }

    private JournalEntry toDomain(JournalEntryEntity e) {
        String content = cipher.decryptFromBase64(new String(e.getContentEnc(), StandardCharsets.UTF_8));
        Instant created = e.getCreatedAt();
        return JournalEntry.reconstitute(
                new JournalEntryId(e.getId()),
                new UserId(e.getUserId()),
                content,
                e.getRelatedCheckinId() == null ? null : new MoodCheckInId(e.getRelatedCheckinId()),
                e.getEntryDate(),
                new AuditMetadata(created, e.getUpdatedAt(), e.getUserId().toString()));
    }
}
