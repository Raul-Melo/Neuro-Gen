package br.com.sentinelcorp.Neuro_Gen.vault.domain.port;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;

/** Porta de auditoria imutável do Cofre (RNF-SEC.8 / RF-12.4). */
public interface VaultAuditLogger {

    void log(UserId userId, String action, String ipAddress, String userAgentHash,
             boolean success, String failureReason);
}
