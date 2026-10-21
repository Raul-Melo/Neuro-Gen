package br.com.sentinelcorp.Neuro_Gen.agenda.domain.model;

import br.com.sentinelcorp.Neuro_Gen.agenda.domain.exception.InvalidRecurrenceRuleException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Regra de recorrência (subconjunto pragmático da RFC 5545, ex.: {@code FREQ=WEEKLY;INTERVAL=1}).
 * Suporta FREQ DAILY/WEEKLY/MONTHLY/YEARLY com INTERVAL para cálculo de próximas ocorrências.
 * Lança {@link InvalidRecurrenceRuleException} se a string for malformada.
 */
public record RRule(String rfc5545String) {

    private static final Pattern FREQ = Pattern.compile("FREQ=(DAILY|WEEKLY|MONTHLY|YEARLY)");

    public RRule {
        if (rfc5545String == null || !FREQ.matcher(rfc5545String).find()) {
            throw new InvalidRecurrenceRuleException(rfc5545String);
        }
    }

    public List<Instant> nextOccurrences(Instant from, int count) {
        Map<String, String> parts = parse();
        String freq = parts.get("FREQ");
        int interval = Integer.parseInt(parts.getOrDefault("INTERVAL", "1"));
        ChronoUnit unit = switch (freq) {
            case "DAILY" -> ChronoUnit.DAYS;
            case "WEEKLY" -> ChronoUnit.WEEKS;
            case "MONTHLY" -> ChronoUnit.MONTHS;
            default -> ChronoUnit.YEARS;
        };
        List<Instant> result = new ArrayList<>();
        Instant cursor = from;
        for (int i = 0; i < count; i++) {
            cursor = cursor.plus(interval, unit);
            result.add(cursor);
        }
        return result;
    }

    public boolean hasEndDate() {
        return rfc5545String.contains("UNTIL=") || rfc5545String.contains("COUNT=");
    }

    private Map<String, String> parse() {
        Map<String, String> parts = new java.util.HashMap<>();
        for (String token : rfc5545String.split(";")) {
            String[] kv = token.split("=", 2);
            if (kv.length == 2) {
                parts.put(kv[0].trim().toUpperCase(), kv[1].trim());
            }
        }
        return parts;
    }
}
