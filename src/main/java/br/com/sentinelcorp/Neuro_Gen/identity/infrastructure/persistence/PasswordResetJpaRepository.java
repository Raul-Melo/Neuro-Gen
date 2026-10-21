package br.com.sentinelcorp.Neuro_Gen.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetJpaRepository extends JpaRepository<PasswordResetEntity, UUID> {

    Optional<PasswordResetEntity> findByTokenHash(String tokenHash);
}
