package br.com.sentinelcorp.Neuro_Gen.identity.infrastructure.service;

import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.EmailSender;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementação de desenvolvimento do {@link EmailSender}: registra o token no log em vez de enviar
 * e-mail real. Em produção seria substituída por um adaptador para o provedor transacional (SES/SendGrid),
 * e o token NUNCA seria logado.
 */
@Component
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void sendEmailVerification(Email to, String rawToken) {
        log.info("[DEV] Verificação de e-mail para {} — token: {}", to.value(), rawToken);
    }

    @Override
    public void sendPasswordReset(Email to, String rawToken) {
        log.info("[DEV] Redefinição de senha para {} — token: {}", to.value(), rawToken);
    }
}
