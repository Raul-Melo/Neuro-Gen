package br.com.sentinelcorp.Neuro_Gen.agenda.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeRangeTest {

    @Test
    void rejectsStartAfterEnd() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> new TimeRange(now.plusSeconds(10), now))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void detectsOverlap() {
        Instant base = Instant.parse("2026-01-01T10:00:00Z");
        TimeRange a = new TimeRange(base, base.plusSeconds(3600));
        TimeRange b = new TimeRange(base.plusSeconds(1800), base.plusSeconds(5400));
        assertThat(a.overlaps(b)).isTrue();
    }
}
