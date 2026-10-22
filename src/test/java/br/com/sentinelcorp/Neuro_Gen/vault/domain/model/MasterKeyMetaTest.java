package br.com.sentinelcorp.Neuro_Gen.vault.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MasterKeyMetaTest {

    @Test
    void verifiesMasterPasswordAndRecoveryKey() {
        MasterKeyMeta meta = MasterKeyMeta.configure(
                UserId.generate(), "m=19456,t=2,p=1", "client-derived-hash", "RECOVERY-KEY-123");

        assertThat(meta.verifyMasterPassword("client-derived-hash")).isTrue();
        assertThat(meta.verifyMasterPassword("wrong")).isFalse();
        assertThat(meta.verifyRecoveryKey("RECOVERY-KEY-123")).isTrue();
        assertThat(meta.verifyRecoveryKey("nope")).isFalse();
    }

    @Test
    void resetMasterPasswordKeepsRecoveryKey() {
        MasterKeyMeta meta = MasterKeyMeta.configure(
                UserId.generate(), "params", "old-hash", "RK");
        MasterKeyMeta reset = meta.withNewMasterPassword("new-hash");

        assertThat(reset.verifyMasterPassword("new-hash")).isTrue();
        assertThat(reset.verifyMasterPassword("old-hash")).isFalse();
        assertThat(reset.verifyRecoveryKey("RK")).isTrue();
    }
}
