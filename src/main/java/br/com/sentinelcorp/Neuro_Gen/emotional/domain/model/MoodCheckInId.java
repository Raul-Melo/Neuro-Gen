package br.com.sentinelcorp.Neuro_Gen.emotional.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;

import java.util.Map;
import java.util.UUID;

public record MoodCheckInId(UUID value) {

    public MoodCheckInId {
        if (value == null) {
            throw new ValidationException(Map.of("moodCheckInId", "não pode ser nulo"));
        }
    }

    public static MoodCheckInId generate() {
        return new MoodCheckInId(UUID.randomUUID());
    }

    public static MoodCheckInId of(String s) {
        return new MoodCheckInId(UUID.fromString(s));
    }

    public String asString() {
        return value.toString();
    }
}
