package br.com.sentinelcorp.Neuro_Gen.identity.application;

import br.com.sentinelcorp.Neuro_Gen.identity.application.command.IdentityCommands.RegisterUserCommand;
import br.com.sentinelcorp.Neuro_Gen.identity.application.result.UserResult;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.exception.AccountAlreadyExistsException;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.Password;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.User;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.EmailSender;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.EmailVerificationTokenRepository;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.PasswordHasher;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.UserRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.application.DomainEventPublisher;
import br.com.sentinelcorp.Neuro_Gen.shared.config.NeuroGenProperties;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationUseCaseTest {

    @Mock UserRepository users;
    @Mock EmailVerificationTokenRepository verificationTokens;
    @Mock PasswordHasher passwordHasher;
    @Mock EmailSender emailSender;
    @Mock DomainEventPublisher events;

    RegistrationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegistrationUseCase(users, verificationTokens, passwordHasher, emailSender,
                events, new NeuroGenProperties());
    }

    @Test
    void registersNewUserAndSendsVerification() {
        when(users.existsByEmail(any(Email.class))).thenReturn(false);
        when(passwordHasher.hash(any())).thenReturn(Password.ofHash("hash"));
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResult result = useCase.register(new RegisterUserCommand("New@Example.com", "senha123", "TITULAR"));

        assertThat(result.email()).isEqualTo("new@example.com");
        assertThat(result.role()).isEqualTo("TITULAR");
        assertThat(result.status()).isEqualTo("PENDING_VERIFICATION");
        verify(verificationTokens).save(any());
        verify(emailSender).sendEmailVerification(any(Email.class), any());
        verify(events).publishFrom(any(User.class));
    }

    @Test
    void rejectsDuplicateEmail() {
        when(users.existsByEmail(any(Email.class))).thenReturn(true);

        assertThatThrownBy(() ->
                useCase.register(new RegisterUserCommand("dup@example.com", "senha123", "APOIO")))
                .isInstanceOf(AccountAlreadyExistsException.class);
    }
}
