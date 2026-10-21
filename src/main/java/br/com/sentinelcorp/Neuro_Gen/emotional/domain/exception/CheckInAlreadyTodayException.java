package br.com.sentinelcorp.Neuro_Gen.emotional.domain.exception;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ConflictException;

/** HTTP 409 — quando a conta está configurada para 1 check-in/dia e já existe um hoje. */
public class CheckInAlreadyTodayException extends ConflictException {
    public CheckInAlreadyTodayException() {
        super("Check-in", "já existe um check-in para hoje");
    }
}
