package br.com.sentinelcorp.Neuro_Gen.agenda.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;

import java.util.Map;
import java.util.UUID;

public record EventId(UUID value) {

    public EventId {
        if (value == null) {
            throw new ValidationException(Map.of("eventId", "não pode ser nulo"));
        }
    }

    public static EventId generate() {
        return new EventId(UUID.randomUUID());
    }

    public static EventId of(String s) {
        try {
            return new EventId(UUID.fromString(s));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(Map.of("eventId", "UUID inválido"));
        }
    }

    public String asString() {
        return value.toString();
    }
}
