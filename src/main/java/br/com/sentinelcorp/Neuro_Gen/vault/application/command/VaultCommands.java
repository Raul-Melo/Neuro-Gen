package br.com.sentinelcorp.Neuro_Gen.vault.application.command;

/** Commands imutáveis do Vault Service. */
public final class VaultCommands {

    private VaultCommands() {
    }

    public record SetupVaultCommand(String userId, String argon2Params, String masterPasswordHash) {
    }

    public record OpenVaultCommand(String userId, String masterPasswordHash) {
    }

    public record CreateVaultEntryCommand(String userId, String label,
                                          String encryptedPayloadBase64Json, String entryType) {
    }

    public record UpdateVaultEntryCommand(String entryId, String userId, String label,
                                          String encryptedPayloadBase64Json) {
    }

    public record RecoverVaultCommand(String userId, String recoveryKey, String newMasterPasswordHash) {
    }
}
