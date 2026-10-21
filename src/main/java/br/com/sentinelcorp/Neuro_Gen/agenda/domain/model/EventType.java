package br.com.sentinelcorp.Neuro_Gen.agenda.domain.model;

public enum EventType {
    /** Compromisso externo (consulta, reunião). */
    APPOINTMENT,
    /** Bloco pessoal (time-blocking). */
    PERSONAL,
    /** Gerado a partir de um RoutineTemplate. */
    ROUTINE
}
