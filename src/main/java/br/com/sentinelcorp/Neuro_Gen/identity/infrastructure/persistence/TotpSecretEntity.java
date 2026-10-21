package br.com.sentinelcorp.Neuro_Gen.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Segredo TOTP cifrado em repouso, 1:1 com o usuário (tabela {@code ng_identity.ng_totp_secrets}). */
@Entity
@Table(name = "ng_totp_secrets", schema = "ng_identity")
@Getter
@Setter
@NoArgsConstructor
public class TotpSecretEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "encrypted_secret", nullable = false)
    private String encryptedSecret;

    @Column(name = "enabled_at", nullable = false)
    private Instant enabledAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
