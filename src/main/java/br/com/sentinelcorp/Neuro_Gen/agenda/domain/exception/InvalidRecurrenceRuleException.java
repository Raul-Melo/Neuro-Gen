package br.com.sentinelcorp.Neuro_Gen.agenda.domain.exception;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.DomainException;
import org.springframework.http.HttpStatus;

/** HTTP 400 — string RRULE malformada. */
public class InvalidRecurrenceRuleException extends DomainException {
    public InvalidRecurrenceRuleException(String invalidRule) {
        super("INVALID_RRULE", "Regra de recorrência inválida: " + invalidRule, HttpStatus.BAD_REQUEST);
    }
}
