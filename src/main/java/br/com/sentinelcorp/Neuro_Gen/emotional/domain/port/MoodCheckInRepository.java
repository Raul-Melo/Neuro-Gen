package br.com.sentinelcorp.Neuro_Gen.emotional.domain.port;

import br.com.sentinelcorp.Neuro_Gen.emotional.domain.model.MoodCheckIn;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.model.MoodCheckInId;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MoodCheckInRepository {

    MoodCheckIn save(MoodCheckIn checkIn);

    Optional<MoodCheckIn> findById(MoodCheckInId id);

    List<MoodCheckIn> findByUserAndPeriod(UserId userId, LocalDate from, LocalDate to);

    Optional<MoodCheckIn> findTodayByUserId(UserId userId);
}
