package br.com.sentinelcorp.Neuro_Gen.shared.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Value Object de e-mail. Valida formato (subconjunto pragmático de RFC 5322) e
 * normaliza para lowercase, garantindo unicidade case-insensitive.
 */
public record Email(String value) {

    private static final Pattern PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public Email {
        if (value == null || value.isBlank()) {
            throw new ValidationException(Map.of("email", "não pode ser vazio"));
        }
        value = value.trim().toLowerCase();
        if (value.length() > 254 || !PATTERN.matcher(value).matches()) {
            throw new ValidationException(Map.of("email", "formato inválido"));
        }
    }
}
