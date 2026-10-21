package br.com.sentinelcorp.Neuro_Gen.agenda.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubtaskJpaRepository extends JpaRepository<SubtaskEntity, UUID> {

    List<SubtaskEntity> findByTaskId(UUID taskId);

    void deleteByTaskId(UUID taskId);
}
