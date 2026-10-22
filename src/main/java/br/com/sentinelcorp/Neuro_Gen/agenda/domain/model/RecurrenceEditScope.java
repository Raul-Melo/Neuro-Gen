package br.com.sentinelcorp.Neuro_Gen.agenda.domain.model;

/** Escopo obrigatório ao editar item recorrente — o sistema sempre pergunta ao usuário (RF-02). */
public enum RecurrenceEditScope {
    THIS_OCCURRENCE,
    THIS_AND_FUTURE,
    ALL_OCCURRENCES
}
