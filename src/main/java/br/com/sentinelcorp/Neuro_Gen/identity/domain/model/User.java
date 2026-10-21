package br.com.sentinelcorp.Neuro_Gen.identity.domain.model;

import br.com.sentinelcorp.Neuro_Gen.identity.domain.event.UserEmailVerified;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.event.UserRegistered;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.exception.AccountSuspendedException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.AggregateRoot;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.UnprocessableEntityException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.AuditMetadata;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.Email;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;

/**
 * Aggregate Root da conta de usuário. Encapsula credenciais, papel e ciclo de vida. A senha nunca
 * é exposta fora do agregado — apenas o hash Argon2id. Registra eventos de domínio que a camada de
 * aplicação despacha após a persistência.
 */
public class User extends AggregateRoot {

    private final UserId id;
    private final Email email;
    private Password passwordHash;
    private final UserRole role;
    private AccountStatus status;
    private boolean emailVerified;
    private TotpSecret totpSecret;  // nullable — 2FA opcional
    private AuditMetadata audit;

    private User(UserId id, Email email, Password passwordHash, UserRole role,
                 AccountStatus status, boolean emailVerified, TotpSecret totpSecret, AuditMetadata audit) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.emailVerified = emailVerified;
        this.totpSecret = totpSecret;
        this.audit = audit;
    }

    /** Fábrica de registro de nova conta (RF-01.1). Conta nasce PENDING_VERIFICATION. */
    public static User register(Email email, Password passwordHash, UserRole role) {
        UserId id = UserId.generate();
        User user = new User(id, email, passwordHash, role,
                AccountStatus.PENDING_VERIFICATION, false, null, AuditMetadata.createNow(id.asString()));
        user.registerEvent(UserRegistered.of(id.asString(), email.value(), role.name()));
        return user;
    }

    /** Reconstrói o agregado a partir da persistência — sem emitir eventos. */
    public static User reconstitute(UserId id, Email email, Password passwordHash, UserRole role,
                                    AccountStatus status, boolean emailVerified, TotpSecret totpSecret,
                                    AuditMetadata audit) {
        return new User(id, email, passwordHash, role, status, emailVerified, totpSecret, audit);
    }

    /** Verifica o e-mail e ativa a conta se estava pendente (RF-01.6). Idempotente. */
    public void verifyEmail() {
        if (emailVerified) {
            return;
        }
        this.emailVerified = true;
        if (status == AccountStatus.PENDING_VERIFICATION) {
            this.status = AccountStatus.ACTIVE;
        }
        this.audit = audit.touch(id.asString());
        registerEvent(UserEmailVerified.of(id.asString()));
    }

    public void changePassword(Password newHash) {
        ensureNotSuspended();
        this.passwordHash = newHash;
        this.audit = audit.touch(id.asString());
    }

    public void enableTotp(TotpSecret secret) {
        ensureNotSuspended();
        if (secret == null) {
            throw new UnprocessableEntityException("TOTP_SECRET_REQUIRED", "Segredo TOTP obrigatório");
        }
        this.totpSecret = secret;
        this.audit = audit.touch(id.asString());
    }

    public void disableTotp() {
        this.totpSecret = null;
        this.audit = audit.touch(id.asString());
    }

    public void suspend(String reason) {
        this.status = AccountStatus.SUSPENDED;
        this.audit = audit.touch("system:" + reason);
    }

    public void requestDeletion() {
        this.status = AccountStatus.DELETED;
        this.audit = audit.touch(id.asString());
    }

    private void ensureNotSuspended() {
        if (status == AccountStatus.SUSPENDED) {
            throw new AccountSuspendedException();
        }
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public boolean requiresTotp() {
        return totpSecret != null;
    }

    public boolean totpEnabled() {
        return totpSecret != null;
    }

    public UserId id() { return id; }
    public Email email() { return email; }
    public Password passwordHash() { return passwordHash; }
    public UserRole role() { return role; }
    public AccountStatus status() { return status; }
    public boolean emailVerified() { return emailVerified; }
    public TotpSecret totpSecret() { return totpSecret; }
    public AuditMetadata audit() { return audit; }
}
