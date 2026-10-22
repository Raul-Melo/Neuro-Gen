package br.com.sentinelcorp.Neuro_Gen.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SessionJpaRepository extends JpaRepository<SessionEntity, UUID> {

    Optional<SessionEntity> findByRefreshTokenHash(String refreshTokenHash);

    @Modifying
    @Query("update SessionEntity s set s.revokedAt = :now "
            + "where s.refreshTokenHash = :hash and s.revokedAt is null")
    void revokeByTokenHash(@Param("hash") String hash, @Param("now") Instant now);

    @Modifying
    @Query("update SessionEntity s set s.revokedAt = :now "
            + "where s.userId = :userId and s.revokedAt is null")
    void revokeAllByUserId(@Param("userId") UUID userId, @Param("now") Instant now);
}
