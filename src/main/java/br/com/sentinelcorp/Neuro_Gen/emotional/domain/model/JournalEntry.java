package br.com.sentinelcorp.Neuro_Gen.emotional.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.AggregateRoot;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.AuditMetadata;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;

import java.time.LocalDate;

/**
 * Aggregate Root do diário emocional (RF-05.3). {@code content} é integralmente cifrado em repouso e
 * NUNCA indexado para busca (PII sensível de saúde).
 */
public class JournalEntry extends AggregateRoot {

    private final JournalEntryId id;
    private final UserId userId;
    private String content;
    private final MoodCheckInId relatedCheckInId;   // nullable
    private final LocalDate entryDate;
    private AuditMetadata audit;

    private JournalEntry(JournalEntryId id, UserId userId, String content, MoodCheckInId relatedCheckInId,
                         LocalDate entryDate, AuditMetadata audit) {
        this.id = id;
        this.userId = userId;
        this.content = content;
        this.relatedCheckInId = relatedCheckInId;
        this.entryDate = entryDate;
        this.audit = audit;
    }

    public static JournalEntry write(UserId userId, String content, MoodCheckInId relatedCheckInId) {
        return new JournalEntry(JournalEntryId.generate(), userId, content, relatedCheckInId,
                LocalDate.now(), AuditMetadata.createNow(userId.asString()));
    }

    public static JournalEntry reconstitute(JournalEntryId id, UserId userId, String content,
                                            MoodCheckInId relatedCheckInId, LocalDate entryDate,
                                            AuditMetadata audit) {
        return new JournalEntry(id, userId, content, relatedCheckInId, entryDate, audit);
    }

    public void edit(String newContent) {
        this.content = newContent;
        this.audit = audit.touch(userId.asString());
    }

    public JournalEntryId id() { return id; }
    public UserId userId() { return userId; }
    public String content() { return content; }
    public MoodCheckInId relatedCheckInId() { return relatedCheckInId; }
    public LocalDate entryDate() { return entryDate; }
    public AuditMetadata audit() { return audit; }
}
