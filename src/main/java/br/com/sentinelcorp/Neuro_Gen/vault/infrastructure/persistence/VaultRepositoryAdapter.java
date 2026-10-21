package br.com.sentinelcorp.Neuro_Gen.vault.infrastructure.persistence;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.AuditMetadata;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.model.EncryptedPayload;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.model.VaultEntry;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.model.VaultEntryId;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.model.VaultEntryType;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.port.VaultRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class VaultRepositoryAdapter implements VaultRepository {

    private final VaultEntryJpaRepository jpa;

    public VaultRepositoryAdapter(VaultEntryJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public VaultEntry save(VaultEntry entry) {
        VaultEntryEntity e = jpa.findById(entry.id().value()).orElseGet(VaultEntryEntity::new);
        e.setId(entry.id().value());
        e.setOwnerId(entry.ownerId().value());
        e.setLabel(entry.label());
        e.setEntryType(entry.entryType().name());
        e.setCiphertext(entry.payload().ciphertext());
        e.setIv(entry.payload().iv());
        e.setSalt(entry.payload().salt());
        if (e.getCreatedAt() == null) {
            e.setCreatedAt(entry.audit().createdAt());
        }
        e.setUpdatedAt(entry.audit().updatedAt());
        jpa.save(e);
        return entry;
    }

    @Override
    public Optional<VaultEntry> findById(VaultEntryId id) {
        return jpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<VaultEntry> findByOwnerId(UserId ownerId) {
        return jpa.findByOwnerId(ownerId.value()).stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(VaultEntryId id) {
        jpa.deleteById(id.value());
    }

    @Override
    public void deleteAllByOwnerId(UserId ownerId) {
        jpa.deleteByOwnerId(ownerId.value());
    }

    private VaultEntry toDomain(VaultEntryEntity e) {
        return VaultEntry.reconstitute(
                new VaultEntryId(e.getId()),
                new UserId(e.getOwnerId()),
                e.getLabel(),
                new EncryptedPayload(e.getCiphertext(), e.getIv(), e.getSalt()),
                VaultEntryType.valueOf(e.getEntryType()),
                new AuditMetadata(e.getCreatedAt(), e.getUpdatedAt(), e.getOwnerId().toString()));
    }
}
