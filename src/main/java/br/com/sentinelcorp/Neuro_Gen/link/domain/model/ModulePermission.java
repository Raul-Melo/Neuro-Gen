package br.com.sentinelcorp.Neuro_Gen.link.domain.model;

/**
 * Módulos que um Titular pode liberar a um Apoio. Ausência de permissão explícita = acesso negado
 * (opt-in — RF-01.3). O Cofre (VAULT) é propositalmente AUSENTE: seu acesso por Apoio é negado de
 * forma estrutural, não configurável (RF-08.5 / ADR-005).
 */
public enum ModulePermission {
    AGENDA,
    EMOTIONAL_CHECKIN,
    JOURNAL,
    FOCUS_HISTORY,
    ORGANIZATION,
    PROGRESS_REPORTS,
    SEND_REMINDERS
}
