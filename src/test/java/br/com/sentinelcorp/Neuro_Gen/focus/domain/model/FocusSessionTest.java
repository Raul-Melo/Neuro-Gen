package br.com.sentinelcorp.Neuro_Gen.focus.domain.model;

import br.com.sentinelcorp.Neuro_Gen.focus.domain.event.FocusSessionEnded;
import br.com.sentinelcorp.Neuro_Gen.focus.domain.exception.SessionNotActiveException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FocusSessionTest {

    private FocusSession start() {
        return FocusSession.start(UserId.generate(), null, 25, 5);
    }

    @Test
    void startsRunningAndEmitsEvent() {
        FocusSession s = start();
        assertThat(s.status()).isEqualTo(SessionStatus.RUNNING);
        assertThat(s.pullDomainEvents()).hasSize(1);
    }

    @Test
    void pauseThenResumeTogglesStatus() {
        FocusSession s = start();
        s.pause();
        assertThat(s.status()).isEqualTo(SessionStatus.PAUSED);
        s.resume();
        assertThat(s.status()).isEqualTo(SessionStatus.RUNNING);
    }

    @Test
    void cannotResumeWhenRunning() {
        FocusSession s = start();
        assertThatThrownBy(s::resume).isInstanceOf(SessionNotActiveException.class);
    }

    @Test
    void completeEndsSessionAndEmitsEvent() {
        FocusSession s = start();
        s.pullDomainEvents();
        s.complete();
        assertThat(s.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(s.endedAt()).isNotNull();
        assertThat(s.effectiveTime().isNegative()).isFalse();
        assertThat(s.pullDomainEvents()).anyMatch(e -> e instanceof FocusSessionEnded);
    }
}
