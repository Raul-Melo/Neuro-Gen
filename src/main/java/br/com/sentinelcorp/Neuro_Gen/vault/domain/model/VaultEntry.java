package br.com.sentinelcorp.Neuro_Gen.vault.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.AggregateRoot;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.AuditMetadata;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;

/**
 * Aggregate Root de uma entrada do Cofre. Contém apenas dado cifrado ({@link EncryptedPayload}) —
 * o servidor armazena e devolve ciphertext, nunca o interpreta.
 */
public class VaultEntry extends AggregateRoot {

    private final VaultEntryId id;
    private final UserId ownerId;
    private String label;
    private EncryptedPayload payload;
    private final VaultEntryType entryType;
    private AuditMetadata audit;

    private VaultEntry(VaultEntryId id, UserId ownerId, String label, EncryptedPayload payload,
                       VaultEntryType entryType, AuditMetadata audit) {
        this.id = id;
        this.ownerId = ownerId;
        this.label = label;
        this.payload = payload;
        this.entryType = entryType;
        this.audit = audit;
    }

    public static VaultEntry create(UserId ownerId, String label, EncryptedPayload payload,
                                    VaultEntryType entryType) {
        return new VaultEntry(VaultEntryId.generate(), ownerId, label, payload, entryType,
                AuditMetadata.createNow(ownerId.asString()));
    }

    public static VaultEntry reconstitute(VaultEntryId id, UserId ownerId, String label,
                                          EncryptedPayload payload, VaultEntryType entryType,
                                          AuditMetadata audit) {
        return new VaultEntry(id, ownerId, label, payload, entryType, audit);
    }

    public void update(String label, EncryptedPayload newPayload) {
        this.label = label;
        this.payload = newPayload;
        this.audit = audit.touch(ownerId.asString());
    }

    public VaultEntryId id() { return id; }
    public UserId ownerId() { return ownerId; }
    public String label() { return label; }
    public EncryptedPayload payload() { return payload; }
    public VaultEntryType entryType() { return entryType; }
    public AuditMetadata audit() { return audit; }
}
