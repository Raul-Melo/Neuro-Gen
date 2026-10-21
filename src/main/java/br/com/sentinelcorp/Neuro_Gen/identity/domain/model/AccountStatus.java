package br.com.sentinelcorp.Neuro_Gen.identity.domain.model;

/** Ciclo de vida da conta. */
public enum AccountStatus {
    /** Cadastrado, aguardando verificação de e-mail. */
    PENDING_VERIFICATION,
    /** Conta operacional. */
    ACTIVE,
    /** Suspensa por violação de TOS ou solicitação. */
    SUSPENDED,
    /** Marcada para exclusão (soft-delete, dados anonimizados). */
    DELETED
}
