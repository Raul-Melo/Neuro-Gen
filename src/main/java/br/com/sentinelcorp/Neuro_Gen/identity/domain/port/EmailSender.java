package br.com.sentinelcorp.Neuro_Gen.identity.domain.port;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.Email;

/**
 * Porta de saída para e-mails transacionais (verificação, reset). A implementação real integra um
 * provedor externo; em dev, uma implementação de log registra o token (RNF-OBS.1: sem PII sensível).
 */
public interface EmailSender {

    void sendEmailVerification(Email to, String rawToken);

    void sendPasswordReset(Email to, String rawToken);
}
