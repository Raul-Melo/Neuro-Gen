package br.com.sentinelcorp.Neuro_Gen.vault.domain.port;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.model.VaultEntry;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.model.VaultEntryId;

import java.util.List;
import java.util.Optional;

public interface VaultRepository {

    VaultEntry save(VaultEntry entry);

    Optional<VaultEntry> findById(VaultEntryId id);

    List<VaultEntry> findByOwnerId(UserId ownerId);

    void delete(VaultEntryId id);

    /** Exclusão imediata e irreversível na deleção de conta (RNF-RET.2). */
    void deleteAllByOwnerId(UserId ownerId);
}
