package br.com.sentinelcorp.Neuro_Gen.identity.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;

import java.util.Map;

/**
 * Value Object que encapsula o hash Argon2id de uma senha (RNF-SEC.5). O texto plano NUNCA é
 * armazenado nem logado — apenas o hash trafega por aqui. O ato de derivar o hash é delegado à
 * porta {@code PasswordHasher} (infra), mantendo o domínio livre de dependências de criptografia.
 */
public record Password(String argon2Hash) {

    public Password {
        if (argon2Hash == null || argon2Hash.isBlank()) {
            throw new ValidationException(Map.of("password", "hash inválido"));
        }
    }

    public static Password ofHash(String argon2Hash) {
        return new Password(argon2Hash);
    }

    /** Regra de complexidade mínima aplicada ao texto plano antes de derivar o hash. */
    public static void validateRawStrength(String raw) {
        if (raw == null || raw.length() < 8 || raw.length() > 128) {
            throw new ValidationException(Map.of("password", "deve ter entre 8 e 128 caracteres"));
        }
        boolean hasLetter = raw.chars().anyMatch(Character::isLetter);
        boolean hasDigit = raw.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new ValidationException(Map.of("password", "deve conter letras e números"));
        }
    }
}
