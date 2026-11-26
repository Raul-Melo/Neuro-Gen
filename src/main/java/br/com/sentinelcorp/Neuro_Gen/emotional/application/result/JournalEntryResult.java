package br.com.sentinelcorp.Neuro_Gen.emotional.application.result;

import br.com.sentinelcorp.Neuro_Gen.emotional.domain.model.JournalEntry;

public record JournalEntryResult(
        String id,
        String content,
        String relatedCheckInId,
        String entryDate,
        String createdAt,
        String updatedAt) {

    public static JournalEntryResult from(JournalEntry e) {
        return new JournalEntryResult(
                e.id().asString(),
                e.content(),
                e.relatedCheckInId() == null ? null : e.relatedCheckInId().asString(),
                e.entryDate().toString(),
                e.audit().createdAt().toString(),
                e.audit().updatedAt().toString());
    }
}
