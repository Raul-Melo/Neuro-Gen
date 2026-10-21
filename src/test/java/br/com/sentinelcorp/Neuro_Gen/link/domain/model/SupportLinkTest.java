package br.com.sentinelcorp.Neuro_Gen.link.domain.model;

import br.com.sentinelcorp.Neuro_Gen.link.domain.event.LinkRevoked;
import br.com.sentinelcorp.Neuro_Gen.link.domain.exception.InviteAlreadyUsedException;
import br.com.sentinelcorp.Neuro_Gen.link.domain.exception.LinkAlreadyRevokedException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.UnprocessableEntityException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SupportLinkTest {

    private SupportLink pendingInvite() {
        return SupportLink.createInvite(UserId.generate(), Duration.ofHours(72));
    }

    @Test
    void createInviteStartsPending() {
        SupportLink link = pendingInvite();
        assertThat(link.status()).isEqualTo(LinkStatus.PENDING);
        assertThat(link.permissions().grantedModules()).isEmpty();
    }

    @Test
    void acceptActivatesTheLink() {
        SupportLink link = pendingInvite();
        link.accept(UserId.generate());
        assertThat(link.status()).isEqualTo(LinkStatus.ACTIVE);
        assertThat(link.isActive()).isTrue();
    }

    @Test
    void acceptTwiceFails() {
        SupportLink link = pendingInvite();
        link.accept(UserId.generate());
        assertThatThrownBy(() -> link.accept(UserId.generate()))
                .isInstanceOf(InviteAlreadyUsedException.class);
    }

    @Test
    void permissionsAreOptInAndGranularPerLink() {
        SupportLink link = pendingInvite();
        link.accept(UserId.generate());
        assertThat(link.hasPermission(ModulePermission.AGENDA)).isFalse();
        link.grantPermission(ModulePermission.AGENDA);
        assertThat(link.hasPermission(ModulePermission.AGENDA)).isTrue();
        assertThat(link.hasPermission(ModulePermission.JOURNAL)).isFalse();
    }

    @Test
    void revokeEmitsEventAndBlocksSecondRevoke() {
        SupportLink link = pendingInvite();
        UserId apoio = UserId.generate();
        link.accept(apoio);
        link.pullDomainEvents();
        link.revoke();
        assertThat(link.status()).isEqualTo(LinkStatus.REVOKED);
        assertThat(link.pullDomainEvents()).anyMatch(e -> e instanceof LinkRevoked);
        assertThatThrownBy(link::revoke).isInstanceOf(LinkAlreadyRevokedException.class);
    }

    @Test
    void grantOnInactiveLinkFails() {
        SupportLink link = pendingInvite();
        assertThatThrownBy(() -> link.grantPermission(ModulePermission.AGENDA))
                .isInstanceOf(UnprocessableEntityException.class);
    }
}
