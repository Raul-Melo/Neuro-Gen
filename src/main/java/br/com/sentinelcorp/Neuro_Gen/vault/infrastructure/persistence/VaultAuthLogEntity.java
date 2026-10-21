package br.com.sentinelcorp.Neuro_Gen.vault.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Log imutável de acesso ao Cofre (tabela {@code ng_vault.ng_vault_auth_logs}). */
@Entity
@Table(name = "ng_vault_auth_logs", schema = "ng_vault")
@Getter
@Setter
@NoArgsConstructor
public class VaultAuthLogEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String action;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent_hash")
    private String userAgentHash;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;
}
