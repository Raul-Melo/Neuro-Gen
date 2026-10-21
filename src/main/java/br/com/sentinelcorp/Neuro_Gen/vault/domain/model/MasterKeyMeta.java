package br.com.sentinelcorp.Neuro_Gen.vault.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import br.com.sentinelcorp.Neuro_Gen.shared.security.Tokens;

import java.time.Instant;

/**
 * Metadados da chave mestra do Cofre. Armazena APENAS os parâmetros Argon2id, um verificador do
 * hash da senha mestra (derivado no cliente) e o hash da chave de recuperação — NUNCA a chave
 * derivada ou a senha mestra (zero-knowledge, RF-08.11).
 */
public record MasterKeyMeta(
        UserId userId,
        String argon2Params,
        String masterVerifierHash,
        String recoveryKeyHash,
        Instant configuredAt,
        Instant lastAuthAt) {

    /** Primeira configuração: deriva verificador e hash da recovery key a partir dos valores do cliente. */
    public static MasterKeyMeta configure(UserId userId, String argon2Params, String masterPasswordHash,
                                          String recoveryKey) {
        return new MasterKeyMeta(userId, argon2Params,
                Tokens.sha256Hex(masterPasswordHash), Tokens.sha256Hex(recoveryKey),
                Instant.now(), null);
    }

    public boolean verifyMasterPassword(String masterPasswordHash) {
        return masterVerifierHash.equals(Tokens.sha256Hex(masterPasswordHash));
    }

    public boolean verifyRecoveryKey(String providedKey) {
        return recoveryKeyHash.equals(Tokens.sha256Hex(providedKey));
    }

    public MasterKeyMeta withNewMasterPassword(String newMasterPasswordHash) {
        return new MasterKeyMeta(userId, argon2Params, Tokens.sha256Hex(newMasterPasswordHash),
                recoveryKeyHash, configuredAt, Instant.now());
    }

    public MasterKeyMeta touchAuth() {
        return new MasterKeyMeta(userId, argon2Params, masterVerifierHash, recoveryKeyHash,
                configuredAt, Instant.now());
    }
}
