package br.com.sentinelcorp.Neuro_Gen.focus.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FocusPauseJpaRepository extends JpaRepository<FocusPauseEntity, UUID> {

    List<FocusPauseEntity> findBySessionIdOrderByPausedAt(UUID sessionId);

    void deleteBySessionId(UUID sessionId);
}
