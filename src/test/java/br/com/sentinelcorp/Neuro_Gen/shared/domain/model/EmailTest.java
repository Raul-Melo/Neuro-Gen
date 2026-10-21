package br.com.sentinelcorp.Neuro_Gen.shared.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void normalizesToLowercaseAndTrims() {
        assertThat(new Email("  User@Example.COM ").value()).isEqualTo("user@example.com");
    }

    @Test
    void rejectsBlank() {
        assertThatThrownBy(() -> new Email("  ")).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsMalformed() {
        assertThatThrownBy(() -> new Email("not-an-email")).isInstanceOf(ValidationException.class);
    }
}
