package br.com.sentinelcorp.Neuro_Gen.vault.domain.exception;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.DomainException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ForbiddenAccessException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.NotFoundException;
import org.springframework.http.HttpStatus;

/** Exceções do Vault Service (agrupadas por coesão do módulo isolado). */
public final class VaultExceptions {

    private VaultExceptions() {
    }

    /** HTTP 401 — sessão do Cofre expirou (auto-lock após inatividade, RF-08.6). */
    public static class VaultSessionExpiredException extends DomainException {
        public VaultSessionExpiredException() {
            super("VAULT_SESSION_EXPIRED", "Sessão do Cofre expirada — autentique novamente", HttpStatus.UNAUTHORIZED);
        }
    }

    /** HTTP 401 — senha mestra incorreta. */
    public static class InvalidMasterKeyException extends DomainException {
        public InvalidMasterKeyException() {
            super("INVALID_MASTER_KEY", "Senha mestra inválida", HttpStatus.UNAUTHORIZED);
        }
    }

    /** HTTP 404 — entrada do Cofre inexistente. */
    public static class VaultEntryNotFoundException extends NotFoundException {
        public VaultEntryNotFoundException(String id) {
            super("Entrada do Cofre", id);
        }
    }

    /** HTTP 403 — tentativa de acesso ao Cofre por conta Apoio (RF-08.5). Sempre auditada. */
    public static class ApoioVaultAccessAttemptException extends ForbiddenAccessException {
        public ApoioVaultAccessAttemptException() {
            super("Cofre", "contas de Apoio não têm acesso ao Cofre em nenhuma hipótese");
        }
    }

    /** HTTP 401 — chave de recuperação não confere (RF-08.11). */
    public static class RecoveryKeyInvalidException extends DomainException {
        public RecoveryKeyInvalidException() {
            super("RECOVERY_KEY_INVALID", "Chave de recuperação inválida", HttpStatus.UNAUTHORIZED);
        }
    }

    /** HTTP 409 — Cofre já configurado para este usuário. */
    public static class VaultAlreadyConfiguredException extends br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ConflictException {
        public VaultAlreadyConfiguredException() {
            super("Cofre", "já configurado");
        }
    }

    /** HTTP 404 — Cofre ainda não configurado para este usuário. */
    public static class VaultNotConfiguredException extends NotFoundException {
        public VaultNotConfiguredException() {
            super("Cofre", "não configurado");
        }
    }
}
