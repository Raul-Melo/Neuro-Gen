package br.com.sentinelcorp.Neuro_Gen.agenda.domain.model;

import java.util.UUID;

/** Identificador opcional de categoria/tag visual. */
public record CategoryId(UUID value) {

    public static CategoryId of(String id) {
        return id == null || id.isBlank() ? null : new CategoryId(UUID.fromString(id));
    }

    public String asString() {
        return value.toString();
    }
}
