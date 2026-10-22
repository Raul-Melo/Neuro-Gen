package br.com.sentinelcorp.Neuro_Gen.vault.infrastructure.persistence;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.port.VaultAuditLogger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** Persiste logs de auditoria no datasource isolado do Vault (sempre, mesmo em falha — REQUIRES_NEW). */
@Component
public class VaultAuditLoggerAdapter implements VaultAuditLogger {

    private final VaultAuthLogJpaRepository jpa;

    public VaultAuditLoggerAdapter(VaultAuthLogJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(value = "vaultTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void log(UserId userId, String action, String ipAddress, String userAgentHash,
                    boolean success, String failureReason) {
        VaultAuthLogEntity e = new VaultAuthLogEntity();
        e.setId(UUID.randomUUID());
        e.setUserId(userId.value());
        e.setAction(action);
        e.setIpAddress(ipAddress);
        e.setUserAgentHash(userAgentHash);
        e.setSuccess(success);
        e.setFailureReason(failureReason);
        e.setAttemptedAt(Instant.now());
        jpa.save(e);
    }
}
