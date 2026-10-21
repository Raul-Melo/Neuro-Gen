package br.com.sentinelcorp.Neuro_Gen.emotional.application.result;

import br.com.sentinelcorp.Neuro_Gen.emotional.domain.model.MoodCheckIn;

public record MoodCheckInResult(
        String id,
        int moodScore,
        int energyLevel,
        String moodLevel,
        String checkInDate,
        String checkInTime,
        String notes) {

    public static MoodCheckInResult from(MoodCheckIn c) {
        return new MoodCheckInResult(
                c.id().asString(),
                c.moodScore().value(),
                c.energyLevel().value(),
                c.moodLevel().name(),
                c.checkInDate().toString(),
                c.checkInTime().toString(),
                c.notes());
    }
}
