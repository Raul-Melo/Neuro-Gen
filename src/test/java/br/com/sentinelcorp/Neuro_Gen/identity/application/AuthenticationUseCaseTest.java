package br.com.sentinelcorp.Neuro_Gen.identity.application;

import br.com.sentinelcorp.Neuro_Gen.identity.application.command.IdentityCommands.LoginCommand;
import br.com.sentinelcorp.Neuro_Gen.identity.application.result.AuthResult;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.exception.InvalidCredentialsException;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.exception.InvalidTotpCodeException;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.exception.TotpRequiredException;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.AccountStatus;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.Password;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.TotpSecret;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.User;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.UserRole;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.PasswordHasher;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.SessionRepository;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.TokenIssuer;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.TotpService;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.UserRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.config.NeuroGenProperties;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.AuditMetadata;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.Email;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationUseCaseTest {

    @Mock UserRepository users;
    @Mock SessionRepository sessions;
    @Mock PasswordHasher passwordHasher;
    @Mock TotpService totpService;
    @Mock TokenIssuer tokenIssuer;

    AuthenticationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AuthenticationUseCase(users, sessions, passwordHasher, totpService,
                tokenIssuer, new NeuroGenProperties());
    }

    private User activeUser(TotpSecret totp) {
        User user = User.reconstitute(UserId.generate(), new Email("user@example.com"),
                Password.ofHash("hash"), UserRole.TITULAR, AccountStatus.ACTIVE, true, totp,
                AuditMetadata.createNow("sys"));
        return user;
    }

    @Test
    void loginSuccessIssuesTokens() {
        User user = activeUser(null);
        when(users.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordHasher.matches(any(), any())).thenReturn(true);
        when(tokenIssuer.issueAccessToken(user)).thenReturn(new TokenIssuer.IssuedAccessToken("acc", 900));

        AuthResult result = useCase.login(new LoginCommand("user@example.com", "senha123", null, "dev"));

        assertThat(result.accessToken()).isEqualTo("acc");
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.user().email()).isEqualTo("user@example.com");
        verify(sessions).save(any());
    }

    @Test
    void loginWithWrongPasswordFails() {
        when(users.findByEmail(any(Email.class))).thenReturn(Optional.of(activeUser(null)));
        when(passwordHasher.matches(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> useCase.login(new LoginCommand("user@example.com", "x", null, "dev")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginRequiresTotpCodeWhenEnabled() {
        when(users.findByEmail(any(Email.class))).thenReturn(Optional.of(activeUser(new TotpSecret("enc"))));
        when(passwordHasher.matches(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> useCase.login(new LoginCommand("user@example.com", "senha123", null, "dev")))
                .isInstanceOf(TotpRequiredException.class);
    }

    @Test
    void loginRejectsInvalidTotpCode() {
        when(users.findByEmail(any(Email.class))).thenReturn(Optional.of(activeUser(new TotpSecret("enc"))));
        when(passwordHasher.matches(any(), any())).thenReturn(true);
        lenient().when(totpService.verify(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> useCase.login(new LoginCommand("user@example.com", "senha123", "000000", "dev")))
                .isInstanceOf(InvalidTotpCodeException.class);
    }
}
