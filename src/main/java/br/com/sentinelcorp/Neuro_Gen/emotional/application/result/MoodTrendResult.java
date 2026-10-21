package br.com.sentinelcorp.Neuro_Gen.emotional.application.result;

import java.util.List;

/**
 * Tendência emocional de um período. {@code trend} e qualquer insight são "padrão observado" —
 * NUNCA diagnóstico. O {@code disclaimer} é obrigatório em telas de insight (RF-05 / RNF-PRIV).
 */
public record MoodTrendResult(
        List<MoodCheckInResult> dataPoints,
        double averageMood,
        double averageEnergy,
        int periodDays,
        String trend,
        String disclaimer) {

    public static final String DISCLAIMER =
            "Este é um padrão observado a partir dos seus registros e NÃO constitui diagnóstico ou "
            + "recomendação médica. Em caso de necessidade, procure um profissional de saúde.";
}
