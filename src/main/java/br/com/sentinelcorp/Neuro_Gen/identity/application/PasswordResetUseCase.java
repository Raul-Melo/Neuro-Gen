package br.com.sentinelcorp.Neuro_Gen.identity.application;

import br.com.sentinelcorp.Neuro_Gen.identity.application.command.IdentityCommands.RequestPasswordResetCommand;
import br.com.sentinelcorp.Neuro_Gen.identity.application.command.IdentityCommands.ResetPasswordCommand;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.exception.TokenExpiredException;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.Password;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.User;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.VerificationToken;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.EmailSender;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.PasswordHasher;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.PasswordResetTokenRepository;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.SessionRepository;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.UserRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.config.NeuroGenProperties;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.Email;
import br.com.sentinelcorp.Neuro_Gen.shared.security.Tokens;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Use case de redefinição de senha via token de e-mail com TTL de 15min (RF-01.7). */
@Service
public class PasswordResetUseCase {

    private final UserRepository users;
    private final PasswordResetTokenRepository resetTokens;
    private final SessionRepository sessions;
    private final PasswordHasher passwordHasher;
    private final EmailSender emailSender;
    private final NeuroGenProperties props;

    public PasswordResetUseCase(UserRepository users, PasswordResetTokenRepository resetTokens,
                                SessionRepository sessions, PasswordHasher passwordHasher,
                                EmailSender emailSender, NeuroGenProperties props) {
        this.users = users;
        this.resetTokens = resetTokens;
        this.sessions = sessions;
        this.passwordHasher = passwordHasher;
        this.emailSender = emailSender;
        this.props = props;
    }

    /** Sempre retorna sem erro — não revela se o e-mail existe (anti-enumeração). */
    @Transactional
    public void requestReset(RequestPasswordResetCommand cmd) {
        users.findByEmail(new Email(cmd.email())).ifPresent(user -> {
            String rawToken = Tokens.random();
            Instant expiresAt = Instant.now().plus(props.getPasswordReset().getTtlMinutes(), ChronoUnit.MINUTES);
            resetTokens.save(VerificationToken.issue(user.id(), Tokens.sha256Hex(rawToken), expiresAt));
            emailSender.sendPasswordReset(user.email(), rawToken);
        });
    }

    @Transactional
    public void reset(ResetPasswordCommand cmd) {
        Password.validateRawStrength(cmd.newRawPassword());
        String hash = Tokens.sha256Hex(cmd.resetToken());
        VerificationToken token = resetTokens.findByTokenHash(hash)
                .filter(VerificationToken::isUsable)
                .orElseThrow(() -> new TokenExpiredException("redefinição de senha"));

        User user = users.findById(token.userId())
                .orElseThrow(() -> new TokenExpiredException("redefinição de senha"));
        user.changePassword(passwordHasher.hash(cmd.newRawPassword()));
        users.save(user);
        resetTokens.markUsed(token);
        // Invalida todas as sessões após troca de senha (defesa em profundidade).
        sessions.revokeAllByUserId(user.id());
    }
}
