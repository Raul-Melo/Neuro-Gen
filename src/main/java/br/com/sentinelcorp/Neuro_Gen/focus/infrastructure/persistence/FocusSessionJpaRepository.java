package br.com.sentinelcorp.Neuro_Gen.focus.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FocusSessionJpaRepository extends JpaRepository<FocusSessionEntity, UUID> {

    Optional<FocusSessionEntity> findFirstByUserIdAndStatusIn(UUID userId, List<String> statuses);

    List<FocusSessionEntity> findByUserIdAndStartedAtBetween(UUID userId, Instant from, Instant to);
}
