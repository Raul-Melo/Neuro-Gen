package br.com.sentinelcorp.Neuro_Gen.focus.infrastructure.persistence;

import br.com.sentinelcorp.Neuro_Gen.focus.domain.model.FocusSession;
import br.com.sentinelcorp.Neuro_Gen.focus.domain.model.FocusSessionId;
import br.com.sentinelcorp.Neuro_Gen.focus.domain.model.PauseRecord;
import br.com.sentinelcorp.Neuro_Gen.focus.domain.model.SessionStatus;
import br.com.sentinelcorp.Neuro_Gen.focus.domain.port.FocusSessionRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.AuditMetadata;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class FocusSessionRepositoryAdapter implements FocusSessionRepository {

    private static final List<String> ONGOING = List.of("RUNNING", "PAUSED");

    private final FocusSessionJpaRepository sessionJpa;
    private final FocusPauseJpaRepository pauseJpa;

    public FocusSessionRepositoryAdapter(FocusSessionJpaRepository sessionJpa, FocusPauseJpaRepository pauseJpa) {
        this.sessionJpa = sessionJpa;
        this.pauseJpa = pauseJpa;
    }

    @Override
    public FocusSession save(FocusSession session) {
        FocusSessionEntity e = sessionJpa.findById(session.id().value()).orElseGet(FocusSessionEntity::new);
        e.setId(session.id().value());
        e.setUserId(session.userId().value());
        e.setTaskId(session.linkedTaskId() == null ? null : UUID.fromString(session.linkedTaskId()));
        e.setPlannedMinutes(session.plannedMinutes());
        e.setBreakMinutes(session.breakMinutes());
        e.setStatus(session.status().name());
        e.setStartedAt(session.startedAt());
        e.setEndedAt(session.endedAt());
        e.setEffectiveSeconds(session.effectiveTime().toSeconds());
        if (e.getCreatedAt() == null) {
            e.setCreatedAt(session.audit().createdAt());
        }
        e.setUpdatedAt(session.audit().updatedAt());
        sessionJpa.save(e);

        pauseJpa.deleteBySessionId(session.id().value());
        for (PauseRecord p : session.pauses()) {
            FocusPauseEntity pe = new FocusPauseEntity();
            pe.setId(UUID.randomUUID());
            pe.setSessionId(session.id().value());
            pe.setPausedAt(p.pausedAt());
            pe.setResumedAt(p.resumedAt());
            pauseJpa.save(pe);
        }
        return session;
    }

    @Override
    public Optional<FocusSession> findById(FocusSessionId id) {
        return sessionJpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<FocusSession> findActiveByUserId(UserId userId) {
        return sessionJpa.findFirstByUserIdAndStatusIn(userId.value(), ONGOING).map(this::toDomain);
    }

    @Override
    public List<FocusSession> findByUserIdAndPeriod(UserId userId, Instant from, Instant to) {
        return sessionJpa.findByUserIdAndStartedAtBetween(userId.value(), from, to)
                .stream().map(this::toDomain).toList();
    }

    private FocusSession toDomain(FocusSessionEntity e) {
        List<PauseRecord> pauses = pauseJpa.findBySessionIdOrderByPausedAt(e.getId()).stream()
                .map(p -> new PauseRecord(p.getPausedAt(), p.getResumedAt()))
                .toList();
        return FocusSession.reconstitute(
                new FocusSessionId(e.getId()),
                new UserId(e.getUserId()),
                e.getTaskId() == null ? null : e.getTaskId().toString(),
                e.getPlannedMinutes(),
                e.getBreakMinutes(),
                e.getStartedAt(),
                e.getEndedAt(),
                SessionStatus.valueOf(e.getStatus()),
                pauses,
                new AuditMetadata(e.getCreatedAt(), e.getUpdatedAt(), e.getUserId().toString()));
    }
}
