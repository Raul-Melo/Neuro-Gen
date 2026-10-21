package br.com.sentinelcorp.Neuro_Gen.link.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;

import java.util.Map;
import java.util.UUID;

public record LinkId(UUID value) {

    public LinkId {
        if (value == null) {
            throw new ValidationException(Map.of("linkId", "não pode ser nulo"));
        }
    }

    public static LinkId generate() {
        return new LinkId(UUID.randomUUID());
    }

    public static LinkId of(String uuidStr) {
        try {
            return new LinkId(UUID.fromString(uuidStr));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(Map.of("linkId", "UUID inválido"));
        }
    }

    public String asString() {
        return value.toString();
    }
}
