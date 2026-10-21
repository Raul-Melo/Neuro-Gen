package br.com.sentinelcorp.Neuro_Gen.shared.domain.model;

import java.time.Instant;

/**
 * Metadados de auditoria imutáveis (RNF-SEC.8 / colunas created_at, updated_at, created_by).
 * {@link #touch(String)} retorna uma nova instância — nunca muta o estado existente.
 */
public record AuditMetadata(Instant createdAt, Instant updatedAt, String createdBy) {

    public static AuditMetadata createNow(String createdBy) {
        Instant now = Instant.now();
        return new AuditMetadata(now, now, createdBy);
    }

    public AuditMetadata touch(String updatedBy) {
        return new AuditMetadata(createdAt, Instant.now(), updatedBy);
    }
}
