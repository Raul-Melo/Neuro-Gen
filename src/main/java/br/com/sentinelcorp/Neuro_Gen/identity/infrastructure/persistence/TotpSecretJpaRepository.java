package br.com.sentinelcorp.Neuro_Gen.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TotpSecretJpaRepository extends JpaRepository<TotpSecretEntity, UUID> {

    Optional<TotpSecretEntity> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);

    @Modifying
    @Query("UPDATE TotpSecretEntity t SET t.setupShown = true WHERE t.userId = :userId")
    void markSetupShown(@Param("userId") UUID userId);
}
