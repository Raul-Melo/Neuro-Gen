package br.com.sentinelcorp.Neuro_Gen.emotional.application;

import br.com.sentinelcorp.Neuro_Gen.emotional.application.command.EmotionalCommands.SubmitMoodCheckInCommand;
import br.com.sentinelcorp.Neuro_Gen.emotional.application.command.EmotionalCommands.WriteJournalCommand;
import br.com.sentinelcorp.Neuro_Gen.emotional.application.result.MoodCheckInResult;
import br.com.sentinelcorp.Neuro_Gen.emotional.application.result.MoodTrendResult;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.model.EnergyLevel;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.model.JournalEntry;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.model.MoodCheckIn;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.model.MoodCheckInId;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.model.MoodScore;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.port.JournalEntryRepository;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.port.MoodCheckInRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.application.SupportAccessPolicy;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Use case de regulação emocional (RF-05). Leitura por conta Apoio passa pela porta
 * {@link SupportAccessPolicy} (permissão EMOTIONAL_CHECKIN concedida pelo Titular — RF-01.3).
 */
@Service
public class EmotionalUseCase {

    private static final String MODULE = "EMOTIONAL_CHECKIN";

    private final MoodCheckInRepository checkIns;
    private final JournalEntryRepository journals;
    private final SupportAccessPolicy accessPolicy;

    public EmotionalUseCase(MoodCheckInRepository checkIns, JournalEntryRepository journals,
                            SupportAccessPolicy accessPolicy) {
        this.checkIns = checkIns;
        this.journals = journals;
        this.accessPolicy = accessPolicy;
    }

    @Transactional
    public MoodCheckInResult submitCheckIn(SubmitMoodCheckInCommand cmd) {
        MoodCheckIn checkIn = MoodCheckIn.record(
                UserId.of(cmd.userId()), new MoodScore(cmd.moodScore()),
                new EnergyLevel(cmd.energyLevel()), cmd.notes());
        return MoodCheckInResult.from(checkIns.save(checkIn));
    }

    @Transactional
    public String writeJournal(WriteJournalCommand cmd) {
        MoodCheckInId related = cmd.relatedCheckInId() == null || cmd.relatedCheckInId().isBlank()
                ? null : MoodCheckInId.of(cmd.relatedCheckInId());
        JournalEntry entry = JournalEntry.write(UserId.of(cmd.userId()), cmd.content(), related);
        return journals.save(entry).id().asString();
    }

    /**
     * Tendência do período. Se o solicitante não for o dono, exige permissão via política de acesso.
     */
    @Transactional(readOnly = true)
    public MoodTrendResult getTrend(String requesterId, String ownerId, int periodDays) {
        accessPolicy.requireModuleAccess(UserId.of(requesterId), UserId.of(ownerId), MODULE);

        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(periodDays);
        List<MoodCheckIn> list = checkIns.findByUserAndPeriod(UserId.of(ownerId), from, to);

        List<MoodCheckInResult> points = list.stream().map(MoodCheckInResult::from).toList();
        double avgMood = list.stream().mapToInt(c -> c.moodScore().value()).average().orElse(0);
        double avgEnergy = list.stream().mapToInt(c -> c.energyLevel().value()).average().orElse(0);
        return new MoodTrendResult(points, round(avgMood), round(avgEnergy), periodDays,
                computeTrend(list), MoodTrendResult.DISCLAIMER);
    }

    private static String computeTrend(List<MoodCheckIn> list) {
        if (list.size() < 2) {
            return "STABLE";
        }
        int mid = list.size() / 2;
        double firstAvg = list.subList(0, mid).stream().mapToInt(c -> c.moodScore().value()).average().orElse(0);
        double secondAvg = list.subList(mid, list.size()).stream()
                .mapToInt(c -> c.moodScore().value()).average().orElse(0);
        double delta = secondAvg - firstAvg;
        if (delta > 0.3) {
            return "IMPROVING";
        }
        return delta < -0.3 ? "DECLINING" : "STABLE";
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
