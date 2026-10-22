package br.com.sentinelcorp.Neuro_Gen.emotional.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JournalEntryJpaRepository extends JpaRepository<JournalEntryEntity, UUID> {

    List<JournalEntryEntity> findByUserIdOrderByEntryDateDesc(UUID userId);
}
