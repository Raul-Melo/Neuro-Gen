package br.com.sentinelcorp.Neuro_Gen.focus.domain.model;

import br.com.sentinelcorp.Neuro_Gen.focus.domain.event.FocusSessionEnded;
import br.com.sentinelcorp.Neuro_Gen.focus.domain.event.FocusSessionStarted;
import br.com.sentinelcorp.Neuro_Gen.focus.domain.exception.SessionNotActiveException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.AggregateRoot;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.AuditMetadata;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate Root de uma sessão de foco (RF-04). {@code effectiveTime} = tempo total decorrido menos
 * a soma das pausas. Permite pausar/retomar sem perder o tempo já acumulado (RF-04.8).
 */
public class FocusSession extends AggregateRoot {

    private final FocusSessionId id;
    private final UserId userId;
    private final String linkedTaskId;     // nullable
    private final int plannedMinutes;
    private final int breakMinutes;
    private final Instant startedAt;
    private Instant endedAt;
    private SessionStatus status;
    private final List<PauseRecord> pauses;
    private AuditMetadata audit;

    private FocusSession(FocusSessionId id, UserId userId, String linkedTaskId, int plannedMinutes,
                         int breakMinutes, Instant startedAt, Instant endedAt, SessionStatus status,
                         List<PauseRecord> pauses, AuditMetadata audit) {
        this.id = id;
        this.userId = userId;
        this.linkedTaskId = linkedTaskId;
        this.plannedMinutes = plannedMinutes;
        this.breakMinutes = breakMinutes;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.status = status;
        this.pauses = new ArrayList<>(pauses);
        this.audit = audit;
    }

    public static FocusSession start(UserId userId, String linkedTaskId, int plannedMinutes, int breakMinutes) {
        FocusSessionId id = FocusSessionId.generate();
        FocusSession session = new FocusSession(id, userId, linkedTaskId, plannedMinutes, breakMinutes,
                Instant.now(), null, SessionStatus.RUNNING, new ArrayList<>(),
                AuditMetadata.createNow(userId.asString()));
        session.registerEvent(FocusSessionStarted.of(id.asString(), userId.asString()));
        return session;
    }

    public static FocusSession reconstitute(FocusSessionId id, UserId userId, String linkedTaskId,
                                            int plannedMinutes, int breakMinutes, Instant startedAt,
                                            Instant endedAt, SessionStatus status, List<PauseRecord> pauses,
                                            AuditMetadata audit) {
        return new FocusSession(id, userId, linkedTaskId, plannedMinutes, breakMinutes, startedAt,
                endedAt, status, pauses, audit);
    }

    public void pause() {
        if (status != SessionStatus.RUNNING) {
            throw new SessionNotActiveException("Só é possível pausar uma sessão em andamento");
        }
        pauses.add(PauseRecord.start(Instant.now()));
        status = SessionStatus.PAUSED;
        audit = audit.touch(userId.asString());
    }

    public void resume() {
        if (status != SessionStatus.PAUSED) {
            throw new SessionNotActiveException("Só é possível retomar uma sessão pausada");
        }
        closeActivePause();
        status = SessionStatus.RUNNING;
        audit = audit.touch(userId.asString());
    }

    public void complete() {
        ensureOngoing();
        closeActivePause();
        endedAt = Instant.now();
        status = SessionStatus.COMPLETED;
        audit = audit.touch(userId.asString());
        registerEvent(FocusSessionEnded.of(id.asString(), userId.asString(), status.name()));
    }

    public void cancel() {
        ensureOngoing();
        closeActivePause();
        endedAt = Instant.now();
        status = SessionStatus.CANCELLED;
        audit = audit.touch(userId.asString());
        registerEvent(FocusSessionEnded.of(id.asString(), userId.asString(), status.name()));
    }

    /** Tempo efetivo = (fim ou agora) - início - soma das pausas. */
    public Duration effectiveTime() {
        Instant end = endedAt == null ? Instant.now() : endedAt;
        Duration total = Duration.between(startedAt, end);
        Duration pausesTotal = pauses.stream()
                .map(PauseRecord::pauseDuration)
                .reduce(Duration.ZERO, Duration::plus);
        Duration effective = total.minus(pausesTotal);
        return effective.isNegative() ? Duration.ZERO : effective;
    }

    private void ensureOngoing() {
        if (status != SessionStatus.RUNNING && status != SessionStatus.PAUSED) {
            throw new SessionNotActiveException("A sessão não está em andamento");
        }
    }

    private void closeActivePause() {
        for (int i = pauses.size() - 1; i >= 0; i--) {
            if (pauses.get(i).isActive()) {
                pauses.set(i, pauses.get(i).end(Instant.now()));
                return;
            }
        }
    }

    public FocusSessionId id() { return id; }
    public UserId userId() { return userId; }
    public String linkedTaskId() { return linkedTaskId; }
    public int plannedMinutes() { return plannedMinutes; }
    public int breakMinutes() { return breakMinutes; }
    public Instant startedAt() { return startedAt; }
    public Instant endedAt() { return endedAt; }
    public SessionStatus status() { return status; }
    public List<PauseRecord> pauses() { return List.copyOf(pauses); }
    public AuditMetadata audit() { return audit; }
}
