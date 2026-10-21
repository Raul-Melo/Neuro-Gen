package br.com.sentinelcorp.Neuro_Gen.focus.domain.model;

public enum SessionStatus {
    /** Sessão ativa — notificações não críticas suprimidas (RF-04.2). */
    RUNNING,
    /** Pausada — notificações normais. */
    PAUSED,
    /** Finalizada pelo usuário. */
    COMPLETED,
    /** Cancelada antes de completar. */
    CANCELLED
}
