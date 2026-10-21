package br.com.sentinelcorp.Neuro_Gen.emotional.domain.exception;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ForbiddenAccessException;

/** HTTP 403 — Apoio sem permissão EMOTIONAL_CHECKIN/JOURNAL tenta acessar dado sensível. */
public class EmotionalDataForbiddenException extends ForbiddenAccessException {
    public EmotionalDataForbiddenException() {
        super("Regulação Emocional", "permissão não concedida pelo Titular");
    }
}
