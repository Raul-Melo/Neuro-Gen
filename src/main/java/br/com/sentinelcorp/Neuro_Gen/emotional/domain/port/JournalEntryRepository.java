package br.com.sentinelcorp.Neuro_Gen.emotional.domain.port;

import br.com.sentinelcorp.Neuro_Gen.emotional.domain.model.JournalEntry;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.model.JournalEntryId;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;

import java.util.List;
import java.util.Optional;

public interface JournalEntryRepository {

    JournalEntry save(JournalEntry entry);

    Optional<JournalEntry> findById(JournalEntryId id);

    List<JournalEntry> findByUserId(UserId userId);
}
