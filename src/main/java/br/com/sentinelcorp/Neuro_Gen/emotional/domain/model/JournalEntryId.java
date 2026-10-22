package br.com.sentinelcorp.Neuro_Gen.emotional.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;

import java.util.Map;
import java.util.UUID;

public record JournalEntryId(UUID value) {

    public JournalEntryId {
        if (value == null) {
            throw new ValidationException(Map.of("journalEntryId", "não pode ser nulo"));
        }
    }

    public static JournalEntryId generate() {
        return new JournalEntryId(UUID.randomUUID());
    }

    public String asString() {
        return value.toString();
    }
}
