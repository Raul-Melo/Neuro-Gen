package br.com.sentinelcorp.Neuro_Gen.link.domain.model;

import br.com.sentinelcorp.Neuro_Gen.link.domain.event.LinkCreated;
import br.com.sentinelcorp.Neuro_Gen.link.domain.event.LinkRevoked;
import br.com.sentinelcorp.Neuro_Gen.link.domain.exception.InviteAlreadyUsedException;
import br.com.sentinelcorp.Neuro_Gen.link.domain.exception.InviteExpiredException;
import br.com.sentinelcorp.Neuro_Gen.link.domain.exception.LinkAlreadyRevokedException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.AggregateRoot;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.UnprocessableEntityException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.AuditMetadata;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;

import java.time.Duration;

/**
 * Aggregate Root do vínculo Titular/Apoio. Controla o ciclo de vida do convite e a matriz de
 * permissões (opt-in). Revogação é imediata e emite {@link LinkRevoked} (RF-01.4).
 */
public class SupportLink extends AggregateRoot {

    private final LinkId id;
    private final UserId titularId;
    private UserId apoioId;            // null enquanto PENDING
    private final InviteToken inviteToken;
    private PermissionMatrix permissions;
    private LinkStatus status;
    private AuditMetadata audit;

    private SupportLink(LinkId id, UserId titularId, UserId apoioId, InviteToken inviteToken,
                        PermissionMatrix permissions, LinkStatus status, AuditMetadata audit) {
        this.id = id;
        this.titularId = titularId;
        this.apoioId = apoioId;
        this.inviteToken = inviteToken;
        this.permissions = permissions;
        this.status = status;
        this.audit = audit;
    }

    /** Cria um convite pendente (RF-01.2). Nenhum módulo é visível até o Titular conceder (opt-in). */
    public static SupportLink createInvite(UserId titularId, Duration inviteTtl) {
        LinkId id = LinkId.generate();
        SupportLink link = new SupportLink(id, titularId, null, InviteToken.generate(inviteTtl),
                PermissionMatrix.empty(), LinkStatus.PENDING, AuditMetadata.createNow(titularId.asString()));
        link.registerEvent(LinkCreated.of(id.asString(), titularId.asString()));
        return link;
    }

    public static SupportLink reconstitute(LinkId id, UserId titularId, UserId apoioId, InviteToken inviteToken,
                                           PermissionMatrix permissions, LinkStatus status, AuditMetadata audit) {
        return new SupportLink(id, titularId, apoioId, inviteToken, permissions, status, audit);
    }

    public void accept(UserId apoioId) {
        if (status == LinkStatus.ACTIVE || this.apoioId != null) {
            throw new InviteAlreadyUsedException();
        }
        if (status == LinkStatus.REVOKED) {
            throw new LinkAlreadyRevokedException();
        }
        if (inviteToken.isExpired()) {
            this.status = LinkStatus.EXPIRED;
            throw new InviteExpiredException();
        }
        if (apoioId.equals(titularId)) {
            throw new UnprocessableEntityException("SELF_LINK", "O Titular não pode se vincular a si mesmo");
        }
        this.apoioId = apoioId;
        this.status = LinkStatus.ACTIVE;
        this.audit = audit.touch(apoioId.asString());
    }

    public void revoke() {
        if (status == LinkStatus.REVOKED) {
            throw new LinkAlreadyRevokedException();
        }
        this.status = LinkStatus.REVOKED;
        this.audit = audit.touch(titularId.asString());
        registerEvent(LinkRevoked.of(id.asString(), titularId.asString(),
                apoioId == null ? null : apoioId.asString()));
    }

    public void expire() {
        this.status = LinkStatus.EXPIRED;
    }

    public void grantPermission(ModulePermission module) {
        ensureActive();
        this.permissions = permissions.grant(module);
        this.audit = audit.touch(titularId.asString());
    }

    public void revokePermission(ModulePermission module) {
        ensureActive();
        this.permissions = permissions.revoke(module);
        this.audit = audit.touch(titularId.asString());
    }

    public boolean hasPermission(ModulePermission module) {
        return isActive() && permissions.has(module);
    }

    public boolean isActive() {
        return status == LinkStatus.ACTIVE;
    }

    public boolean isExpired() {
        return inviteToken.isExpired();
    }

    private void ensureActive() {
        if (!isActive()) {
            throw new UnprocessableEntityException("LINK_NOT_ACTIVE",
                    "Permissões só podem ser alteradas em vínculo ativo");
        }
    }

    public LinkId id() { return id; }
    public UserId titularId() { return titularId; }
    public UserId apoioId() { return apoioId; }
    public InviteToken inviteToken() { return inviteToken; }
    public PermissionMatrix permissions() { return permissions; }
    public LinkStatus status() { return status; }
    public AuditMetadata audit() { return audit; }
}
