package br.com.sentinelcorp.Neuro_Gen.identity.infrastructure.persistence;

import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.TotpSetupStatePort;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class TotpSetupStateAdapter implements TotpSetupStatePort {

    private final TotpSecretJpaRepository totpJpa;

    public TotpSetupStateAdapter(TotpSecretJpaRepository totpJpa) {
        this.totpJpa = totpJpa;
    }

    @Override
    public Optional<String> findEncryptedSecretIfNotShown(UserId userId) {
        return totpJpa.findByUserId(userId.value())
                .filter(e -> !e.isSetupShown())
                .map(TotpSecretEntity::getEncryptedSecret);
    }

    @Override
    public void markSetupShown(UserId userId) {
        totpJpa.markSetupShown(userId.value());
    }

    @Override
    public void resetSetupShown(UserId userId) {
        totpJpa.findByUserId(userId.value()).ifPresent(e -> {
            e.setSetupShown(false);
            totpJpa.save(e);
        });
    }
}
