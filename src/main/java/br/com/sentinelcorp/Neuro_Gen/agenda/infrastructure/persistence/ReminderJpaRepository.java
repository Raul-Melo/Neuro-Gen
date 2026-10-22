package br.com.sentinelcorp.Neuro_Gen.agenda.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReminderJpaRepository extends JpaRepository<ReminderEntity, UUID> {

    List<ReminderEntity> findByEventId(UUID eventId);

    List<ReminderEntity> findByTaskId(UUID taskId);

    void deleteByEventId(UUID eventId);

    void deleteByTaskId(UUID taskId);
}
