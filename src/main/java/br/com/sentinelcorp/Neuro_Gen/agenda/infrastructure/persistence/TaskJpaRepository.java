package br.com.sentinelcorp.Neuro_Gen.agenda.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskJpaRepository extends JpaRepository<TaskEntity, UUID> {

    List<TaskEntity> findByOwnerId(UUID ownerId);

    List<TaskEntity> findByOwnerIdAndStatus(UUID ownerId, String status);
}
