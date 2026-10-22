package br.com.sentinelcorp.Neuro_Gen.link.application;

import br.com.sentinelcorp.Neuro_Gen.link.application.command.LinkCommands.AcceptInviteCommand;
import br.com.sentinelcorp.Neuro_Gen.link.application.command.LinkCommands.CreateInviteCommand;
import br.com.sentinelcorp.Neuro_Gen.link.application.result.InviteResult;
import br.com.sentinelcorp.Neuro_Gen.link.application.result.LinkResult;
import br.com.sentinelcorp.Neuro_Gen.link.domain.model.SupportLink;
import br.com.sentinelcorp.Neuro_Gen.link.domain.port.SupportLinkRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.application.DomainEventPublisher;
import br.com.sentinelcorp.Neuro_Gen.shared.config.NeuroGenProperties;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.NotFoundException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import br.com.sentinelcorp.Neuro_Gen.shared.security.Tokens;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/** Use case de criação e aceite de convite de vínculo (RF-01.2). */
@Service
public class InviteUseCase {

    private final SupportLinkRepository links;
    private final DomainEventPublisher events;
    private final NeuroGenProperties props;

    public InviteUseCase(SupportLinkRepository links, DomainEventPublisher events, NeuroGenProperties props) {
        this.links = links;
        this.events = events;
        this.props = props;
    }

    @Transactional
    public InviteResult create(CreateInviteCommand cmd) {
        Duration ttl = Duration.ofHours(props.getInvite().getTtlHours());
        SupportLink link = SupportLink.createInvite(UserId.of(cmd.titularId()), ttl);
        links.save(link);
        events.publishFrom(link);
        String raw = link.inviteToken().value();
        return new InviteResult(
                link.id().asString(),
                raw,
                "neurogen://invite/accept?token=" + raw,
                link.inviteToken().expiresAt().toString());
    }

    @Transactional
    public LinkResult accept(AcceptInviteCommand cmd) {
        String hash = Tokens.sha256Hex(cmd.rawInviteToken());
        SupportLink link = links.findByTokenHash(hash)
                .orElseThrow(() -> new NotFoundException("Convite", "token"));
        link.accept(UserId.of(cmd.apoioId()));
        links.save(link);
        events.publishFrom(link);
        return LinkResult.from(link);
    }
}
