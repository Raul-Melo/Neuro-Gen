package br.com.sentinelcorp.Neuro_Gen.focus.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;

import java.util.Map;
import java.util.UUID;

public record FocusSessionId(UUID value) {

    public FocusSessionId {
        if (value == null) {
            throw new ValidationException(Map.of("focusSessionId", "não pode ser nulo"));
        }
    }

    public static FocusSessionId generate() {
        return new FocusSessionId(UUID.randomUUID());
    }

    public static FocusSessionId of(String s) {
        try {
            return new FocusSessionId(UUID.fromString(s));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(Map.of("focusSessionId", "UUID inválido"));
        }
    }

    public String asString() {
        return value.toString();
    }
}
