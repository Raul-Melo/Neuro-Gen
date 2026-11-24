package br.com.sentinelcorp.Neuro_Gen.shared.audit;

/** Port de saída: persiste um SystemLogEntry na tabela ng_audit.ng_system_logs. */
public interface SystemLogPort {
    void append(SystemLogEntry entry);
}
