package br.com.sentinelcorp.Neuro_Gen.shared.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;

import java.util.Map;
import java.util.UUID;

/**
 * Value Object de identidade de usuário. Compartilhado por todos os módulos (um {@code User} do
 * módulo Identity é referenciado por Link, Agenda, Focus, Emotional e Vault apenas pelo seu id).
 */
public record UserId(UUID value) {

    public UserId {
        if (value == null) {
            throw new ValidationException(Map.of("userId", "não pode ser nulo"));
        }
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId of(String uuidStr) {
        try {
            return new UserId(UUID.fromString(uuidStr));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(Map.of("userId", "UUID inválido: " + uuidStr));
        }
    }

    public String asString() {
        return value.toString();
    }
}
