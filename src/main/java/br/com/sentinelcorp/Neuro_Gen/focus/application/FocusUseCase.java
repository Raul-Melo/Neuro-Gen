package br.com.sentinelcorp.Neuro_Gen.focus.application;

import br.com.sentinelcorp.Neuro_Gen.focus.domain.exception.SessionAlreadyActiveException;
import br.com.sentinelcorp.Neuro_Gen.focus.domain.model.FocusSession;
import br.com.sentinelcorp.Neuro_Gen.focus.domain.model.FocusSessionId;
import br.com.sentinelcorp.Neuro_Gen.focus.domain.port.FocusSessionRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.application.DomainEventPublisher;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ForbiddenAccessException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.NotFoundException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Use case de sessões de foco (RF-04): start/pause/resume/complete/cancel + histórico. */
@Service
public class FocusUseCase {

    private final FocusSessionRepository sessions;
    private final DomainEventPublisher events;

    public FocusUseCase(FocusSessionRepository sessions, DomainEventPublisher events) {
        this.sessions = sessions;
        this.events = events;
    }

    @Transactional
    public FocusSessionResult start(String userId, String linkedTaskId, int durationMinutes, int breakMinutes) {
        UserId uid = UserId.of(userId);
        if (sessions.findActiveByUserId(uid).isPresent()) {
            throw new SessionAlreadyActiveException();
        }
        FocusSession session = FocusSession.start(uid, linkedTaskId, durationMinutes, breakMinutes);
        sessions.save(session);
        events.publishFrom(session);
        return FocusSessionResult.from(session);
    }

    @Transactional
    public FocusSessionResult pause(String sessionId, String userId) {
        FocusSession session = load(sessionId, userId);
        session.pause();
        return FocusSessionResult.from(sessions.save(session));
    }

    @Transactional
    public FocusSessionResult resume(String sessionId, String userId) {
        FocusSession session = load(sessionId, userId);
        session.resume();
        return FocusSessionResult.from(sessions.save(session));
    }

    @Transactional
    public FocusSessionResult complete(String sessionId, String userId) {
        FocusSession session = load(sessionId, userId);
        session.complete();
        sessions.save(session);
        events.publishFrom(session);
        return FocusSessionResult.from(session);
    }

    @Transactional
    public FocusSessionResult cancel(String sessionId, String userId) {
        FocusSession session = load(sessionId, userId);
        session.cancel();
        sessions.save(session);
        events.publishFrom(session);
        return FocusSessionResult.from(session);
    }

    @Transactional(readOnly = true)
    public Optional<FocusSessionResult> current(String userId) {
        return sessions.findActiveByUserId(UserId.of(userId)).map(FocusSessionResult::from);
    }

    @Transactional(readOnly = true)
    public List<FocusSessionResult> history(String userId, Instant from, Instant to) {
        return sessions.findByUserIdAndPeriod(UserId.of(userId), from, to)
                .stream().map(FocusSessionResult::from).toList();
    }

    private FocusSession load(String sessionId, String userId) {
        FocusSession session = sessions.findById(FocusSessionId.of(sessionId))
                .orElseThrow(() -> new NotFoundException("Sessão de foco", sessionId));
        if (!session.userId().equals(UserId.of(userId))) {
            throw new ForbiddenAccessException("Sessão de foco", "não pertence ao usuário");
        }
        return session;
    }
}
