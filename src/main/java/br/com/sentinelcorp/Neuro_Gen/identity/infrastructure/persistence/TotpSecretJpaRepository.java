package br.com.sentinelcorp.Neuro_Gen.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TotpSecretJpaRepository extends JpaRepository<TotpSecretEntity, UUID> {

    Optional<TotpSecretEntity> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
