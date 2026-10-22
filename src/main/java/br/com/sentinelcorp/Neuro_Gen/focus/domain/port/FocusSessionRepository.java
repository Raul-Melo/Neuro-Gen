package br.com.sentinelcorp.Neuro_Gen.focus.domain.port;

import br.com.sentinelcorp.Neuro_Gen.focus.domain.model.FocusSession;
import br.com.sentinelcorp.Neuro_Gen.focus.domain.model.FocusSessionId;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FocusSessionRepository {

    FocusSession save(FocusSession session);

    Optional<FocusSession> findById(FocusSessionId id);

    /** Sessão em andamento (RUNNING ou PAUSED) do usuário, se houver. */
    Optional<FocusSession> findActiveByUserId(UserId userId);

    List<FocusSession> findByUserIdAndPeriod(UserId userId, Instant from, Instant to);
}
