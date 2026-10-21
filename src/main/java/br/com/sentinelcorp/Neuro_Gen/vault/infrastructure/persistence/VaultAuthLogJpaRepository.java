package br.com.sentinelcorp.Neuro_Gen.vault.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VaultAuthLogJpaRepository extends JpaRepository<VaultAuthLogEntity, UUID> {
}
