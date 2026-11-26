package br.com.sentinelcorp.Neuro_Gen.emotional.application;

import br.com.sentinelcorp.Neuro_Gen.emotional.application.command.EmotionalCommands.DeleteCheckInCommand;
import br.com.sentinelcorp.Neuro_Gen.emotional.application.command.EmotionalCommands.DeleteJournalCommand;
import br.com.sentinelcorp.Neuro_Gen.emotional.application.command.EmotionalCommands.EditJournalCommand;
import br.com.sentinelcorp.Neuro_Gen.emotional.application.command.EmotionalCommands.SubmitMoodCheckInCommand;
import br.com.sentinelcorp.Neuro_Gen.emotional.application.command.EmotionalCommands.WriteJournalCommand;
import br.com.sentinelcorp.Neuro_Gen.emotional.application.result.JournalEntryResult;
import br.com.sentinelcorp.Neuro_Gen.emotional.application.result.MoodCheckInResult;
import br.com.sentinelcorp.Neuro_Gen.emotional.application.result.MoodTrendResult;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.exception.CheckInAlreadyTodayException;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.exception.EmotionalDataForbiddenException;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.model.EnergyLevel;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.model.JournalEntry;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.model.JournalEntryId;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.model.MoodCheckIn;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.model.MoodCheckInId;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.model.MoodScore;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.port.JournalEntryRepository;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.port.MoodCheckInRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.application.SupportAccessPolicy;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

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
        UserId userId = UserId.of(cmd.userId());
        if (checkIns.findTodayByUserId(userId).isPresent()) {
            throw new CheckInAlreadyTodayException();
        }
        MoodCheckIn checkIn = MoodCheckIn.record(
                userId, new MoodScore(cmd.moodScore()),
                new EnergyLevel(cmd.energyLevel()), cmd.notes());
        return MoodCheckInResult.from(checkIns.save(checkIn));
    }

    @Transactional(readOnly = true)
    public MoodCheckInResult getCheckIn(String requesterId, String checkInId) {
        MoodCheckIn c = checkIns.findById(MoodCheckInId.of(checkInId))
                .orElseThrow(() -> new NotFoundException("Check-in", checkInId));
        requireOwnership(requesterId, c.userId().asString());
        return MoodCheckInResult.from(c);
    }

    @Transactional(readOnly = true)
    public List<MoodCheckInResult> listCheckIns(String requesterId, int periodDays) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(periodDays);
        return checkIns.findByUserAndPeriod(UserId.of(requesterId), from, to)
                .stream().map(MoodCheckInResult::from).toList();
    }

    @Transactional
    public void deleteCheckIn(DeleteCheckInCommand cmd) {
        MoodCheckIn c = checkIns.findById(MoodCheckInId.of(cmd.checkInId()))
                .orElseThrow(() -> new NotFoundException("Check-in", cmd.checkInId()));
        requireOwnership(cmd.userId(), c.userId().asString());
        checkIns.delete(c.id());
    }

    @Transactional
    public JournalEntryResult writeJournal(WriteJournalCommand cmd) {
        MoodCheckInId related = cmd.relatedCheckInId() == null || cmd.relatedCheckInId().isBlank()
                ? null : MoodCheckInId.of(cmd.relatedCheckInId());
        JournalEntry entry = JournalEntry.write(UserId.of(cmd.userId()), cmd.content(), related);
        return JournalEntryResult.from(journals.save(entry));
    }

    @Transactional(readOnly = true)
    public JournalEntryResult getJournalEntry(String requesterId, String entryId) {
        JournalEntry e = journals.findById(JournalEntryId.of(entryId))
                .orElseThrow(() -> new NotFoundException("Entrada de diário", entryId));
        requireOwnership(requesterId, e.userId().asString());
        return JournalEntryResult.from(e);
    }

    @Transactional(readOnly = true)
    public List<JournalEntryResult> listJournalEntries(String requesterId) {
        return journals.findByUserId(UserId.of(requesterId))
                .stream().map(JournalEntryResult::from).toList();
    }

    @Transactional
    public JournalEntryResult editJournalEntry(EditJournalCommand cmd) {
        JournalEntry e = journals.findById(JournalEntryId.of(cmd.entryId()))
                .orElseThrow(() -> new NotFoundException("Entrada de diário", cmd.entryId()));
        requireOwnership(cmd.userId(), e.userId().asString());
        e.edit(cmd.content());
        return JournalEntryResult.from(journals.save(e));
    }

    @Transactional
    public void deleteJournalEntry(DeleteJournalCommand cmd) {
        JournalEntry e = journals.findById(JournalEntryId.of(cmd.entryId()))
                .orElseThrow(() -> new NotFoundException("Entrada de diário", cmd.entryId()));
        requireOwnership(cmd.userId(), e.userId().asString());
        journals.delete(e.id());
    }

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

    private static void requireOwnership(String requesterId, String ownerId) {
        if (!requesterId.equals(ownerId)) {
            throw new EmotionalDataForbiddenException();
        }
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
