package br.com.sentinelcorp.Neuro_Gen.vault.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VaultEntryJpaRepository extends JpaRepository<VaultEntryEntity, UUID> {

    List<VaultEntryEntity> findByOwnerId(UUID ownerId);

    void deleteByOwnerId(UUID ownerId);
}
