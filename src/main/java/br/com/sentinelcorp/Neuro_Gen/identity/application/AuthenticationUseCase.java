package br.com.sentinelcorp.Neuro_Gen.identity.application;

import br.com.sentinelcorp.Neuro_Gen.identity.application.command.IdentityCommands.LoginCommand;
import br.com.sentinelcorp.Neuro_Gen.identity.application.command.IdentityCommands.LogoutCommand;
import br.com.sentinelcorp.Neuro_Gen.identity.application.command.IdentityCommands.RefreshTokenCommand;
import br.com.sentinelcorp.Neuro_Gen.identity.application.result.AuthResult;
import br.com.sentinelcorp.Neuro_Gen.identity.application.result.UserResult;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.exception.AccountSuspendedException;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.exception.InvalidCredentialsException;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.exception.InvalidTotpCodeException;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.exception.TokenExpiredException;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.exception.TotpRequiredException;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.AccountStatus;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.RefreshSession;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.User;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.PasswordHasher;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.SessionRepository;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.TokenIssuer;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.TotpService;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.UserRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.config.NeuroGenProperties;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.Email;
import br.com.sentinelcorp.Neuro_Gen.shared.security.Tokens;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Use case de autenticação: login (com 2FA opcional), refresh com rotação e logout. */
@Service
public class AuthenticationUseCase {

    private final UserRepository users;
    private final SessionRepository sessions;
    private final PasswordHasher passwordHasher;
    private final TotpService totpService;
    private final TokenIssuer tokenIssuer;
    private final NeuroGenProperties props;

    public AuthenticationUseCase(UserRepository users, SessionRepository sessions,
                                 PasswordHasher passwordHasher, TotpService totpService,
                                 TokenIssuer tokenIssuer, NeuroGenProperties props) {
        this.users = users;
        this.sessions = sessions;
        this.passwordHasher = passwordHasher;
        this.totpService = totpService;
        this.tokenIssuer = tokenIssuer;
        this.props = props;
    }

    @Transactional
    public AuthResult login(LoginCommand cmd) {
        User user = users.findByEmail(new Email(cmd.email()))
                .orElseThrow(InvalidCredentialsException::new);

        if (user.status() == AccountStatus.SUSPENDED) {
            throw new AccountSuspendedException();
        }
        if (user.status() == AccountStatus.DELETED
                || !passwordHasher.matches(cmd.rawPassword(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        verifyTotpIfRequired(user, cmd.totpCode());

        return issueTokens(user, cmd.deviceInfo());
    }

    @Transactional
    public AuthResult refresh(RefreshTokenCommand cmd) {
        String hash = Tokens.sha256Hex(cmd.refreshToken());
        RefreshSession session = sessions.findByTokenHash(hash)
                .filter(RefreshSession::isActive)
                .orElseThrow(() -> new TokenExpiredException("refresh"));

        User user = users.findById(session.userId())
                .orElseThrow(() -> new TokenExpiredException("refresh"));

        // Rotação: a sessão antiga é revogada e uma nova é emitida.
        sessions.revokeByTokenHash(hash);
        return issueTokens(user, cmd.deviceInfo());
    }

    @Transactional
    public void logout(LogoutCommand cmd) {
        sessions.revokeByTokenHash(Tokens.sha256Hex(cmd.refreshToken()));
    }

    private void verifyTotpIfRequired(User user, String totpCode) {
        if (!user.requiresTotp()) {
            return;
        }
        if (totpCode == null || totpCode.isBlank()) {
            throw new TotpRequiredException();
        }
        if (!totpService.verify(user.totpSecret(), totpCode)) {
            throw new InvalidTotpCodeException();
        }
    }

    private AuthResult issueTokens(User user, String deviceInfo) {
        TokenIssuer.IssuedAccessToken access = tokenIssuer.issueAccessToken(user);
        String rawRefresh = Tokens.random();
        Instant expiresAt = Instant.now()
                .plus(props.getJwt().getRefreshTokenTtlSeconds(), ChronoUnit.SECONDS);
        sessions.save(RefreshSession.issue(user.id(), Tokens.sha256Hex(rawRefresh), expiresAt, deviceInfo));
        return new AuthResult(access.token(), rawRefresh, access.expiresInSeconds(), UserResult.from(user));
    }
}
