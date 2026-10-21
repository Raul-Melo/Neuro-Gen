package br.com.sentinelcorp.Neuro_Gen.identity.application;

import br.com.sentinelcorp.Neuro_Gen.identity.application.command.IdentityCommands.RegisterUserCommand;
import br.com.sentinelcorp.Neuro_Gen.identity.application.command.IdentityCommands.VerifyEmailCommand;
import br.com.sentinelcorp.Neuro_Gen.identity.application.result.UserResult;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.exception.AccountAlreadyExistsException;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.exception.TokenExpiredException;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.Password;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.User;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.UserRole;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.VerificationToken;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.EmailSender;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.EmailVerificationTokenRepository;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.PasswordHasher;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.UserRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.application.DomainEventPublisher;
import br.com.sentinelcorp.Neuro_Gen.shared.config.NeuroGenProperties;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.Email;
import br.com.sentinelcorp.Neuro_Gen.shared.security.Tokens;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/** Use case de registro de conta (RF-01.1) e verificação de e-mail (RF-01.6). */
@Service
public class RegistrationUseCase {

    private final UserRepository users;
    private final EmailVerificationTokenRepository verificationTokens;
    private final PasswordHasher passwordHasher;
    private final EmailSender emailSender;
    private final DomainEventPublisher events;
    private final NeuroGenProperties props;

    public RegistrationUseCase(UserRepository users, EmailVerificationTokenRepository verificationTokens,
                               PasswordHasher passwordHasher, EmailSender emailSender,
                               DomainEventPublisher events, NeuroGenProperties props) {
        this.users = users;
        this.verificationTokens = verificationTokens;
        this.passwordHasher = passwordHasher;
        this.emailSender = emailSender;
        this.events = events;
        this.props = props;
    }

    @Transactional
    public UserResult register(RegisterUserCommand cmd) {
        Email email = new Email(cmd.email());
        UserRole role = parseRole(cmd.role());
        Password.validateRawStrength(cmd.rawPassword());

        if (users.existsByEmail(email)) {
            throw new AccountAlreadyExistsException(email.value());
        }

        Password hash = passwordHasher.hash(cmd.rawPassword());
        User user = User.register(email, hash, role);
        User saved = users.save(user);
        events.publishFrom(user);

        issueAndSendVerification(saved);
        return UserResult.from(saved);
    }

    @Transactional
    public UserResult verifyEmail(VerifyEmailCommand cmd) {
        String hash = Tokens.sha256Hex(cmd.token());
        VerificationToken token = verificationTokens.findByTokenHash(hash)
                .filter(VerificationToken::isUsable)
                .orElseThrow(() -> new TokenExpiredException("verificação de e-mail"));

        User user = users.findById(token.userId())
                .orElseThrow(() -> new TokenExpiredException("verificação de e-mail"));
        user.verifyEmail();
        User saved = users.save(user);
        verificationTokens.markUsed(token);
        events.publishFrom(user);
        return UserResult.from(saved);
    }

    private void issueAndSendVerification(User user) {
        String rawToken = Tokens.random();
        Instant expiresAt = Instant.now().plus(props.getEmailVerification().getTtlHours(), ChronoUnit.HOURS);
        verificationTokens.save(VerificationToken.issue(user.id(), Tokens.sha256Hex(rawToken), expiresAt));
        emailSender.sendEmailVerification(user.email(), rawToken);
    }

    private static UserRole parseRole(String role) {
        try {
            return UserRole.valueOf(role);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ValidationException(Map.of("role", "deve ser TITULAR ou APOIO"));
        }
    }
}
