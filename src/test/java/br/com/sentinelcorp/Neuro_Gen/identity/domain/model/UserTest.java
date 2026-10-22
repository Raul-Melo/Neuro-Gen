package br.com.sentinelcorp.Neuro_Gen.identity.domain.model;

import br.com.sentinelcorp.Neuro_Gen.identity.domain.event.UserEmailVerified;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.event.UserRegistered;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.Email;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private User newUser() {
        return User.register(new Email("user@example.com"), Password.ofHash("hash"), UserRole.TITULAR);
    }

    @Test
    void registrationStartsPendingAndEmitsEvent() {
        User user = newUser();
        assertThat(user.status()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
        assertThat(user.emailVerified()).isFalse();
        assertThat(user.pullDomainEvents()).hasSize(1).first().isInstanceOf(UserRegistered.class);
    }

    @Test
    void verifyEmailActivatesAndEmitsEvent() {
        User user = newUser();
        user.pullDomainEvents(); // descarta o UserRegistered
        user.verifyEmail();
        assertThat(user.emailVerified()).isTrue();
        assertThat(user.isActive()).isTrue();
        assertThat(user.pullDomainEvents()).anyMatch(e -> e instanceof UserEmailVerified);
    }

    @Test
    void verifyEmailIsIdempotent() {
        User user = newUser();
        user.verifyEmail();
        user.pullDomainEvents();
        user.verifyEmail();
        assertThat(user.pullDomainEvents()).isEmpty();
    }

    @Test
    void enablingTotpMakesItRequired() {
        User user = newUser();
        assertThat(user.requiresTotp()).isFalse();
        user.enableTotp(new TotpSecret("encrypted-secret"));
        assertThat(user.requiresTotp()).isTrue();
        user.disableTotp();
        assertThat(user.requiresTotp()).isFalse();
    }
}
