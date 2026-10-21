package br.com.sentinelcorp.Neuro_Gen.identity.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordTest {

    @Test
    void acceptsStrongPassword() {
        assertThatCode(() -> Password.validateRawStrength("senha123")).doesNotThrowAnyException();
    }

    @Test
    void rejectsTooShort() {
        assertThatThrownBy(() -> Password.validateRawStrength("ab1"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsWithoutDigit() {
        assertThatThrownBy(() -> Password.validateRawStrength("somenteletras"))
                .isInstanceOf(ValidationException.class);
    }
}
