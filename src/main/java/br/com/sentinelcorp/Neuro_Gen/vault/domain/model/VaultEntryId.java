package br.com.sentinelcorp.Neuro_Gen.vault.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;

import java.util.Map;
import java.util.UUID;

public record VaultEntryId(UUID value) {

    public VaultEntryId {
        if (value == null) {
            throw new ValidationException(Map.of("vaultEntryId", "não pode ser nulo"));
        }
    }

    public static VaultEntryId generate() {
        return new VaultEntryId(UUID.randomUUID());
    }

    public static VaultEntryId of(String s) {
        try {
            return new VaultEntryId(UUID.fromString(s));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(Map.of("vaultEntryId", "UUID inválido"));
        }
    }

    public String asString() {
        return value.toString();
    }
}
