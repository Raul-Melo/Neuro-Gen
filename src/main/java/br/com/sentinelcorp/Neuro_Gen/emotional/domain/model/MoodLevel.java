package br.com.sentinelcorp.Neuro_Gen.emotional.domain.model;

/**
 * Nível de humor derivado do {@code MoodScore}. Apresentado em dashboards como "padrão observado",
 * NUNCA como diagnóstico ou recomendação médica (RF-05 / RNF-PRIV.1).
 */
public enum MoodLevel {
    VERY_LOW,
    LOW,
    NEUTRAL,
    HIGH,
    VERY_HIGH;

    public static MoodLevel fromScore(int score) {
        return switch (score) {
            case 1 -> VERY_LOW;
            case 2 -> LOW;
            case 3 -> NEUTRAL;
            case 4 -> HIGH;
            case 5 -> VERY_HIGH;
            default -> throw new IllegalArgumentException("Score fora de 1..5: " + score);
        };
    }
}
