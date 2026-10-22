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

/** Metadados da chave mestra (tabela {@code ng_vault.ng_master_key_meta}, 1:1 com usuário). */
@Entity
@Table(name = "ng_master_key_meta", schema = "ng_vault")
@Getter
@Setter
@NoArgsConstructor
public class MasterKeyMetaEntity {

    @Id
    private UUID id;   // = user_id

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "argon2_params", nullable = false)
    private String argon2Params;

    @Column(name = "master_verifier_hash", nullable = false)
    private String masterVerifierHash;

    @Column(name = "recovery_key_hash", nullable = false)
    private String recoveryKeyHash;

    @Column(name = "configured_at", nullable = false)
    private Instant configuredAt;

    @Column(name = "last_auth_at")
    private Instant lastAuthAt;
}
