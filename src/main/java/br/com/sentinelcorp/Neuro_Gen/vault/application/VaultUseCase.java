package br.com.sentinelcorp.Neuro_Gen.vault.application;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import br.com.sentinelcorp.Neuro_Gen.shared.security.Tokens;
import br.com.sentinelcorp.Neuro_Gen.vault.application.command.VaultCommands.CreateVaultEntryCommand;
import br.com.sentinelcorp.Neuro_Gen.vault.application.command.VaultCommands.OpenVaultCommand;
import br.com.sentinelcorp.Neuro_Gen.vault.application.command.VaultCommands.RecoverVaultCommand;
import br.com.sentinelcorp.Neuro_Gen.vault.application.command.VaultCommands.SetupVaultCommand;
import br.com.sentinelcorp.Neuro_Gen.vault.application.command.VaultCommands.UpdateVaultEntryCommand;
import br.com.sentinelcorp.Neuro_Gen.vault.application.result.VaultResults.VaultEntryResult;
import br.com.sentinelcorp.Neuro_Gen.vault.application.result.VaultResults.VaultSessionResult;
import br.com.sentinelcorp.Neuro_Gen.vault.application.result.VaultResults.VaultSetupResult;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.exception.VaultExceptions.InvalidMasterKeyException;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.exception.VaultExceptions.RecoveryKeyInvalidException;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.exception.VaultExceptions.VaultAlreadyConfiguredException;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.exception.VaultExceptions.VaultEntryNotFoundException;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.exception.VaultExceptions.VaultNotConfiguredException;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.model.EncryptedPayload;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.model.MasterKeyMeta;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.model.VaultEntry;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.model.VaultEntryId;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.model.VaultEntryType;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.port.MasterKeyMetaRepository;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.port.VaultAuditLogger;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.port.VaultRepository;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.port.VaultSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use case do Cofre zero-knowledge (EPIC-08). O servidor manipula apenas ciphertext. Toda operação
 * é registrada na auditoria imutável; o acesso por Apoio é bloqueado estruturalmente antes daqui
 * (cadeia de segurança do Vault — RF-08.5).
 *
 * <p>Transações usam o {@code vaultTransactionManager} (datasource isolado — ADR-005).</p>
 */
@Service
@Transactional("vaultTransactionManager")
public class VaultUseCase {

    private final VaultRepository entries;
    private final MasterKeyMetaRepository masterKeys;
    private final VaultSessionService sessions;
    private final VaultAuditLogger audit;

    public VaultUseCase(VaultRepository entries, MasterKeyMetaRepository masterKeys,
                        VaultSessionService sessions, VaultAuditLogger audit) {
        this.entries = entries;
        this.masterKeys = masterKeys;
        this.sessions = sessions;
        this.audit = audit;
    }

    public VaultSetupResult setup(SetupVaultCommand cmd) {
        UserId userId = UserId.of(cmd.userId());
        if (masterKeys.findByUserId(userId).isPresent()) {
            throw new VaultAlreadyConfiguredException();
        }
        String recoveryKey = Tokens.random(24);
        masterKeys.save(MasterKeyMeta.configure(userId, cmd.argon2Params(), cmd.masterPasswordHash(), recoveryKey));
        audit.log(userId, "SETUP", null, null, true, null);
        return new VaultSetupResult(recoveryKey);
    }

    public VaultSessionResult open(OpenVaultCommand cmd) {
        UserId userId = UserId.of(cmd.userId());
        MasterKeyMeta meta = masterKeys.findByUserId(userId).orElseThrow(VaultNotConfiguredException::new);
        if (!meta.verifyMasterPassword(cmd.masterPasswordHash())) {
            audit.log(userId, "OPEN", null, null, false, "invalid master key");
            throw new InvalidMasterKeyException();
        }
        masterKeys.save(meta.touchAuth());
        VaultSessionService.OpenedSession session = sessions.open(userId);
        audit.log(userId, "OPEN", null, null, true, null);
        return new VaultSessionResult(session.token(), session.expiresInSeconds());
    }

    public void recover(RecoverVaultCommand cmd) {
        UserId userId = UserId.of(cmd.userId());
        MasterKeyMeta meta = masterKeys.findByUserId(userId).orElseThrow(VaultNotConfiguredException::new);
        if (!meta.verifyRecoveryKey(cmd.recoveryKey())) {
            audit.log(userId, "RECOVER", null, null, false, "invalid recovery key");
            throw new RecoveryKeyInvalidException();
        }
        masterKeys.save(meta.withNewMasterPassword(cmd.newMasterPasswordHash()));
        audit.log(userId, "RECOVER", null, null, true, null);
    }

    public VaultEntryResult createEntry(String vaultSessionToken, CreateVaultEntryCommand cmd) {
        UserId userId = requireSession(vaultSessionToken);
        VaultEntry entry = VaultEntry.create(userId, cmd.label(),
                EncryptedPayload.fromBase64Json(cmd.encryptedPayloadBase64Json()),
                VaultEntryType.valueOf(cmd.entryType()));
        entries.save(entry);
        audit.log(userId, "ENTRY_CREATE", null, null, true, null);
        return VaultEntryResult.from(entry);
    }

    public VaultEntryResult updateEntry(String vaultSessionToken, UpdateVaultEntryCommand cmd) {
        UserId userId = requireSession(vaultSessionToken);
        VaultEntry entry = loadOwned(cmd.entryId(), userId);
        entry.update(cmd.label(), EncryptedPayload.fromBase64Json(cmd.encryptedPayloadBase64Json()));
        entries.save(entry);
        audit.log(userId, "ENTRY_UPDATE", null, null, true, null);
        return VaultEntryResult.from(entry);
    }

    @Transactional(value = "vaultTransactionManager", readOnly = true)
    public List<VaultEntryResult> listEntries(String vaultSessionToken) {
        UserId userId = requireSession(vaultSessionToken);
        audit.log(userId, "ENTRY_READ", null, null, true, null);
        return entries.findByOwnerId(userId).stream().map(VaultEntryResult::from).toList();
    }

    public void deleteEntry(String vaultSessionToken, String entryId) {
        UserId userId = requireSession(vaultSessionToken);
        loadOwned(entryId, userId);
        entries.delete(VaultEntryId.of(entryId));
        audit.log(userId, "ENTRY_DELETE", null, null, true, null);
    }

    public void lock(String vaultSessionToken) {
        sessions.invalidate(vaultSessionToken);
    }

    private UserId requireSession(String vaultSessionToken) {
        return sessions.validate(vaultSessionToken);
    }

    private VaultEntry loadOwned(String entryId, UserId userId) {
        VaultEntry entry = entries.findById(VaultEntryId.of(entryId))
                .orElseThrow(() -> new VaultEntryNotFoundException(entryId));
        if (!entry.ownerId().equals(userId)) {
            throw new VaultEntryNotFoundException(entryId); // não revela existência de outro dono
        }
        return entry;
    }
}
