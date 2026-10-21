package br.com.sentinelcorp.Neuro_Gen.vault.application.result;

import br.com.sentinelcorp.Neuro_Gen.vault.domain.model.VaultEntry;

/** Resultados do Vault Service. */
public final class VaultResults {

    private VaultResults() {
    }

    /** Exibido UMA ÚNICA VEZ no setup — sem a recovery key, o Cofre é irrecuperável (RF-08.11). */
    public record VaultSetupResult(String recoveryKey) {
    }

    public record VaultSessionResult(String vaultSessionToken, long expiresInSeconds) {
    }

    public record VaultEntryResult(String id, String label, String entryType,
                                   String encryptedPayload, String createdAt, String updatedAt) {

        public static VaultEntryResult from(VaultEntry e) {
            return new VaultEntryResult(
                    e.id().asString(), e.label(), e.entryType().name(),
                    e.payload().toBase64Json(),
                    e.audit().createdAt().toString(), e.audit().updatedAt().toString());
        }
    }
}
