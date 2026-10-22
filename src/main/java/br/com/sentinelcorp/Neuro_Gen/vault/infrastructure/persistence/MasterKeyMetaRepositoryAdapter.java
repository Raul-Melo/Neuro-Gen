package br.com.sentinelcorp.Neuro_Gen.vault.infrastructure.persistence;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.model.MasterKeyMeta;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.port.MasterKeyMetaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MasterKeyMetaRepositoryAdapter implements MasterKeyMetaRepository {

    private final MasterKeyMetaJpaRepository jpa;

    public MasterKeyMetaRepositoryAdapter(MasterKeyMetaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public MasterKeyMeta save(MasterKeyMeta meta) {
        MasterKeyMetaEntity e = jpa.findById(meta.userId().value()).orElseGet(MasterKeyMetaEntity::new);
        e.setId(meta.userId().value());
        e.setUserId(meta.userId().value());
        e.setArgon2Params(meta.argon2Params());
        e.setMasterVerifierHash(meta.masterVerifierHash());
        e.setRecoveryKeyHash(meta.recoveryKeyHash());
        e.setConfiguredAt(meta.configuredAt());
        e.setLastAuthAt(meta.lastAuthAt());
        jpa.save(e);
        return meta;
    }

    @Override
    public Optional<MasterKeyMeta> findByUserId(UserId userId) {
        return jpa.findByUserId(userId.value()).map(e -> new MasterKeyMeta(
                new UserId(e.getUserId()), e.getArgon2Params(), e.getMasterVerifierHash(),
                e.getRecoveryKeyHash(), e.getConfiguredAt(), e.getLastAuthAt()));
    }
}
