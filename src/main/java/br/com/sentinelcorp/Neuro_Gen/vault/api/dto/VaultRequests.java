package br.com.sentinelcorp.Neuro_Gen.vault.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Request DTOs do Cofre. Todos os payloads já chegam cifrados do cliente (zero-knowledge). */
public final class VaultRequests {

    private VaultRequests() {
    }

    public record SetupVaultRequest(
            @NotBlank String argon2Params,
            @NotBlank String masterPasswordHash) {
    }

    public record OpenVaultRequest(@NotBlank String masterPasswordHash) {
    }

    public record RecoverVaultRequest(
            @NotBlank String recoveryKey,
            @NotBlank String newMasterPasswordHash) {
    }

    public record CreateEntryRequest(
            @NotBlank String label,
            @NotBlank String encryptedPayload,
            @NotBlank String entryType) {
    }

    public record UpdateEntryRequest(
            @NotBlank String label,
            @NotBlank String encryptedPayload) {
    }
}
