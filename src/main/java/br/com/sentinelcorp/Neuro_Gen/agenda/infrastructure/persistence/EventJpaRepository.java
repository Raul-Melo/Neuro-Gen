package br.com.sentinelcorp.Neuro_Gen.agenda.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EventJpaRepository extends JpaRepository<EventEntity, UUID> {

    List<EventEntity> findByOwnerIdAndStartAtBetween(UUID ownerId, Instant from, Instant to);
}
