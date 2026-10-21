package br.com.sentinelcorp.Neuro_Gen.emotional.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;

import java.util.Map;

/** Pontuação de humor na escala visual 1..5 (RF-05.1). */
public record MoodScore(int value) {

    public MoodScore {
        if (value < 1 || value > 5) {
            throw new ValidationException(Map.of("moodScore", "deve estar entre 1 e 5"));
        }
    }

    public MoodLevel toLevel() {
        return MoodLevel.fromScore(value);
    }
}
