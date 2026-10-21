package br.com.sentinelcorp.Neuro_Gen.emotional.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.AggregateRoot;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.AuditMetadata;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Aggregate Root do check-in de humor/energia (RF-05.1). Dado sensível de saúde: {@code notes} é
 * cifrado em repouso (pgcrypto/AES-256-GCM) na camada de infraestrutura.
 */
public class MoodCheckIn extends AggregateRoot {

    private final MoodCheckInId id;
    private final UserId userId;
    private final MoodScore moodScore;
    private final EnergyLevel energyLevel;
    private final LocalDate checkInDate;
    private final LocalTime checkInTime;
    private final String notes;            // nullable — cifrado em repouso
    private final AuditMetadata audit;

    private MoodCheckIn(MoodCheckInId id, UserId userId, MoodScore moodScore, EnergyLevel energyLevel,
                        LocalDate checkInDate, LocalTime checkInTime, String notes, AuditMetadata audit) {
        this.id = id;
        this.userId = userId;
        this.moodScore = moodScore;
        this.energyLevel = energyLevel;
        this.checkInDate = checkInDate;
        this.checkInTime = checkInTime;
        this.notes = notes;
        this.audit = audit;
    }

    public static MoodCheckIn record(UserId userId, MoodScore moodScore, EnergyLevel energyLevel, String notes) {
        return new MoodCheckIn(MoodCheckInId.generate(), userId, moodScore, energyLevel,
                LocalDate.now(), LocalTime.now(), notes, AuditMetadata.createNow(userId.asString()));
    }

    public static MoodCheckIn reconstitute(MoodCheckInId id, UserId userId, MoodScore moodScore,
                                           EnergyLevel energyLevel, LocalDate checkInDate, LocalTime checkInTime,
                                           String notes, AuditMetadata audit) {
        return new MoodCheckIn(id, userId, moodScore, energyLevel, checkInDate, checkInTime, notes, audit);
    }

    public boolean isBelowThreshold(int threshold) {
        return moodScore.value() < threshold;
    }

    public MoodLevel moodLevel() {
        return moodScore.toLevel();
    }

    public MoodCheckInId id() { return id; }
    public UserId userId() { return userId; }
    public MoodScore moodScore() { return moodScore; }
    public EnergyLevel energyLevel() { return energyLevel; }
    public LocalDate checkInDate() { return checkInDate; }
    public LocalTime checkInTime() { return checkInTime; }
    public String notes() { return notes; }
    public AuditMetadata audit() { return audit; }
}
