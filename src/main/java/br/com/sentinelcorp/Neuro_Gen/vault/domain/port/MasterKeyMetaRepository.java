package br.com.sentinelcorp.Neuro_Gen.vault.domain.port;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.model.MasterKeyMeta;

import java.util.Optional;

public interface MasterKeyMetaRepository {

    MasterKeyMeta save(MasterKeyMeta meta);

    Optional<MasterKeyMeta> findByUserId(UserId userId);
}
