package br.com.sentinelcorp.Neuro_Gen.emotional.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;

import java.util.Map;

/** Nível de energia na mesma escala do humor (1..5). */
public record EnergyLevel(int value) {

    public EnergyLevel {
        if (value < 1 || value > 5) {
            throw new ValidationException(Map.of("energyLevel", "deve estar entre 1 e 5"));
        }
    }
}
