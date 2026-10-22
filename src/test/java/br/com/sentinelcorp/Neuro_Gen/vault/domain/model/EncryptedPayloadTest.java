package br.com.sentinelcorp.Neuro_Gen.vault.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptedPayloadTest {

    @Test
    void roundTripsThroughBase64Json() {
        EncryptedPayload payload = new EncryptedPayload(
                new byte[]{1, 2, 3}, new byte[]{4, 5}, new byte[]{6, 7, 8, 9});
        String serialized = payload.toBase64Json();
        EncryptedPayload parsed = EncryptedPayload.fromBase64Json(serialized);
        assertThat(parsed.ciphertext()).containsExactly(1, 2, 3);
        assertThat(parsed.iv()).containsExactly(4, 5);
        assertThat(parsed.salt()).containsExactly(6, 7, 8, 9);
    }

    @Test
    void rejectsMalformedPayload() {
        assertThatThrownBy(() -> EncryptedPayload.fromBase64Json("only.two"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsEmptyComponents() {
        assertThatThrownBy(() -> new EncryptedPayload(new byte[0], new byte[]{1}, new byte[]{1}))
                .isInstanceOf(ValidationException.class);
    }
}
