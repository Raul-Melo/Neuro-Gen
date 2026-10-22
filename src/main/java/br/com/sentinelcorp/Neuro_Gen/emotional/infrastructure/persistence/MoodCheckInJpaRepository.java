package br.com.sentinelcorp.Neuro_Gen.emotional.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MoodCheckInJpaRepository extends JpaRepository<MoodCheckInEntity, UUID> {

    List<MoodCheckInEntity> findByUserIdAndCheckinDateBetweenOrderByCheckinDateAscCheckinTimeAsc(
            UUID userId, LocalDate from, LocalDate to);

    Optional<MoodCheckInEntity> findFirstByUserIdAndCheckinDate(UUID userId, LocalDate date);
}
