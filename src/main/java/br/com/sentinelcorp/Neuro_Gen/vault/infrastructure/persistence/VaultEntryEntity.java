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

/** Entrada do Cofre (tabela {@code ng_vault.ng_vault_entries}). Apenas bytes cifrados. */
@Entity
@Table(name = "ng_vault_entries", schema = "ng_vault")
@Getter
@Setter
@NoArgsConstructor
public class VaultEntryEntity {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String label;

    @Column(name = "entry_type", nullable = false)
    private String entryType;

    @Column(nullable = false)
    private byte[] ciphertext;

    @Column(nullable = false)
    private byte[] iv;

    @Column(nullable = false)
    private byte[] salt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
