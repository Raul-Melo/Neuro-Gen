package br.com.sentinelcorp.Neuro_Gen.vault.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MasterKeyMetaJpaRepository extends JpaRepository<MasterKeyMetaEntity, UUID> {

    Optional<MasterKeyMetaEntity> findByUserId(UUID userId);
}
