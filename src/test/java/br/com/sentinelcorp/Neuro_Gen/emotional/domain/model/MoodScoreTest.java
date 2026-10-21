package br.com.sentinelcorp.Neuro_Gen.emotional.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoodScoreTest {

    @Test
    void rejectsOutOfRange() {
        assertThatThrownBy(() -> new MoodScore(0)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new MoodScore(6)).isInstanceOf(ValidationException.class);
    }

    @Test
    void mapsToLevel() {
        assertThat(new MoodScore(1).toLevel()).isEqualTo(MoodLevel.VERY_LOW);
        assertThat(new MoodScore(3).toLevel()).isEqualTo(MoodLevel.NEUTRAL);
        assertThat(new MoodScore(5).toLevel()).isEqualTo(MoodLevel.VERY_HIGH);
    }
}
