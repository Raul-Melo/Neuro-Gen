package br.com.sentinelcorp.Neuro_Gen.identity.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;

import java.util.Map;

/**
 * Value Object para CPF brasileiro. Armazena apenas os 11 dígitos numéricos.
 * Valida o algoritmo dos dígitos verificadores e rejeita sequências uniformes (111.111.111-11).
 */
public record Cpf(String digits) {

    private static final int[] WEIGHT1 = {10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHT2 = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};

    public Cpf {
        if (digits == null || !digits.matches("\\d{11}")) {
            throw new ValidationException(Map.of("cpf", "CPF inválido"));
        }
        if (digits.chars().distinct().count() == 1) {
            throw new ValidationException(Map.of("cpf", "CPF inválido"));
        }
        int d1 = checkDigit(digits, WEIGHT1);
        int d2 = checkDigit(digits, WEIGHT2);
        if (d1 != Character.getNumericValue(digits.charAt(9))
                || d2 != Character.getNumericValue(digits.charAt(10))) {
            throw new ValidationException(Map.of("cpf", "CPF inválido"));
        }
    }

    /** Aceita CPF com ou sem máscara (000.000.000-00 ou 00000000000). */
    public static Cpf of(String raw) {
        if (raw == null) return null;
        return new Cpf(raw.replaceAll("[^\\d]", ""));
    }

    /** Retorna o CPF no formato 000.000.000-00. */
    public String masked() {
        return digits.substring(0, 3) + "." + digits.substring(3, 6) + "."
                + digits.substring(6, 9) + "-" + digits.substring(9);
    }

    private static int checkDigit(String cpf, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += Character.getNumericValue(cpf.charAt(i)) * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
