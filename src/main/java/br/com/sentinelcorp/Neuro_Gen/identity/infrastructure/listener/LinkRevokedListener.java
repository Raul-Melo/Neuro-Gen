package br.com.sentinelcorp.Neuro_Gen.identity.infrastructure.listener;

import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.SessionRepository;
import br.com.sentinelcorp.Neuro_Gen.link.domain.event.LinkRevoked;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Reage a {@link LinkRevoked} (contrato público do módulo Link) invalidando imediatamente as sessões
 * ativas do Apoio (RF-01.4: propagação &lt; 5s). Executa de forma síncrona na mesma transação da
 * revogação, garantindo atomicidade entre revogar o vínculo e derrubar as sessões.
 */
@Component
public class LinkRevokedListener {

    private static final Logger log = LoggerFactory.getLogger(LinkRevokedListener.class);

    private final SessionRepository sessions;

    public LinkRevokedListener(SessionRepository sessions) {
        this.sessions = sessions;
    }

    @EventListener
    public void onLinkRevoked(LinkRevoked event) {
        if (event.apoioId() == null) {
            return; // vínculo revogado ainda pendente (sem Apoio vinculado)
        }
        sessions.revokeAllByUserId(UserId.of(event.apoioId()));
        log.info("Sessões do Apoio {} invalidadas após revogação do vínculo {}",
                event.apoioId(), event.linkId());
    }
}
