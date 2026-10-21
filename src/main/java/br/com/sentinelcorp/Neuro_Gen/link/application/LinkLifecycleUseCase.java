package br.com.sentinelcorp.Neuro_Gen.link.application;

import br.com.sentinelcorp.Neuro_Gen.link.application.command.LinkCommands.RevokeLinkCommand;
import br.com.sentinelcorp.Neuro_Gen.link.application.result.LinkResult;
import br.com.sentinelcorp.Neuro_Gen.link.domain.model.LinkId;
import br.com.sentinelcorp.Neuro_Gen.link.domain.model.SupportLink;
import br.com.sentinelcorp.Neuro_Gen.link.domain.port.SupportLinkRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.application.DomainEventPublisher;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ForbiddenAccessException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.NotFoundException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Use case de revogação (RF-01.4) e listagem de vínculos. */
@Service
public class LinkLifecycleUseCase {

    private final SupportLinkRepository links;
    private final DomainEventPublisher events;

    public LinkLifecycleUseCase(SupportLinkRepository links, DomainEventPublisher events) {
        this.links = links;
        this.events = events;
    }

    @Transactional
    public void revoke(RevokeLinkCommand cmd) {
        SupportLink link = links.findById(LinkId.of(cmd.linkId()))
                .orElseThrow(() -> new NotFoundException("Vínculo", cmd.linkId()));
        if (!link.titularId().equals(UserId.of(cmd.titularId()))) {
            throw new ForbiddenAccessException("Vínculo", "apenas o Titular dono pode revogar");
        }
        link.revoke();
        links.save(link);
        events.publishFrom(link);
    }

    @Transactional(readOnly = true)
    public List<LinkResult> listByTitular(UserId titularId) {
        return links.findActiveByTitularId(titularId).stream().map(LinkResult::from).toList();
    }

    @Transactional(readOnly = true)
    public List<LinkResult> listByApoio(UserId apoioId) {
        return links.findActiveByApoioId(apoioId).stream().map(LinkResult::from).toList();
    }
}
