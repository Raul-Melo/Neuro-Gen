package br.com.sentinelcorp.Neuro_Gen.link.domain.model;

public enum LinkStatus {
    /** Convite enviado, aguardando aceite. */
    PENDING,
    /** Vínculo ativo com permissões configuráveis. */
    ACTIVE,
    /** Encerrado pelo Titular (RF-01.4). */
    REVOKED,
    /** Token de convite não aceito dentro do TTL de 72h. */
    EXPIRED
}
