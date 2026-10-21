package br.com.sentinelcorp.Neuro_Gen.agenda.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;

import java.util.Map;
import java.util.UUID;

public record TaskId(UUID value) {

    public TaskId {
        if (value == null) {
            throw new ValidationException(Map.of("taskId", "não pode ser nulo"));
        }
    }

    public static TaskId generate() {
        return new TaskId(UUID.randomUUID());
    }

    public static TaskId of(String s) {
        try {
            return new TaskId(UUID.fromString(s));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(Map.of("taskId", "UUID inválido"));
        }
    }

    public String asString() {
        return value.toString();
    }
}
