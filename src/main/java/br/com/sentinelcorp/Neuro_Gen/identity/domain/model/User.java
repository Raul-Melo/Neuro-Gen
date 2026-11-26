package br.com.sentinelcorp.Neuro_Gen.identity.domain.model;

import br.com.sentinelcorp.Neuro_Gen.identity.domain.event.UserEmailVerified;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.event.UserRegistered;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.exception.AccountSuspendedException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.AggregateRoot;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.UnprocessableEntityException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.AuditMetadata;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.Email;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;

import java.time.LocalDate;

/**
 * Aggregate Root da conta de usuário. A senha nunca é exposta fora do agregado — apenas o hash
 * Argon2id. CPF e data de nascimento são dados obrigatórios no cadastro.
 * O TOTP só é exigido no login após o usuário confirmar o código (totpConfirmed = true).
 */
public class User extends AggregateRoot {

    private final UserId id;
    private final Email email;
    private Password passwordHash;
    private final UserRole role;
    private AccountStatus status;
    private boolean emailVerified;
    private final Cpf cpf;
    private final LocalDate birthDate;
    private TotpSecret totpSecret;      // nullable — 2FA pendente ou ativo
    private boolean totpConfirmed;      // true somente após confirmação com código válido
    private AuditMetadata audit;

    private User(UserId id, Email email, Password passwordHash, UserRole role,
                 AccountStatus status, boolean emailVerified, Cpf cpf, LocalDate birthDate,
                 TotpSecret totpSecret, boolean totpConfirmed, AuditMetadata audit) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.emailVerified = emailVerified;
        this.cpf = cpf;
        this.birthDate = birthDate;
        this.totpSecret = totpSecret;
        this.totpConfirmed = totpConfirmed;
        this.audit = audit;
    }

    /** Fábrica de registro de nova conta (RF-01.1). Conta nasce PENDING_VERIFICATION. */
    public static User register(Email email, Password passwordHash, UserRole role,
                                Cpf cpf, LocalDate birthDate) {
        UserId id = UserId.generate();
        User user = new User(id, email, passwordHash, role,
                AccountStatus.PENDING_VERIFICATION, false, cpf, birthDate,
                null, false, AuditMetadata.createNow(id.asString()));
        user.registerEvent(UserRegistered.of(id.asString(), email.value(), role.name()));
        return user;
    }

    /** Reconstrói o agregado a partir da persistência — sem emitir eventos. */
    public static User reconstitute(UserId id, Email email, Password passwordHash, UserRole role,
                                    AccountStatus status, boolean emailVerified, Cpf cpf,
                                    LocalDate birthDate, TotpSecret totpSecret, boolean totpConfirmed,
                                    AuditMetadata audit) {
        return new User(id, email, passwordHash, role, status, emailVerified, cpf, birthDate,
                totpSecret, totpConfirmed, audit);
    }

    /** Verifica o e-mail e ativa a conta se estava pendente (RF-01.6). Idempotente. */
    public void verifyEmail() {
        if (emailVerified) return;
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

    /** Armazena o segredo pendente. 2FA NÃO é exigido no login até {@link #confirmTotp()} ser chamado. */
    public void enableTotp(TotpSecret secret) {
        ensureNotSuspended();
        if (secret == null) {
            throw new UnprocessableEntityException("TOTP_SECRET_REQUIRED", "Segredo TOTP obrigatório");
        }
        this.totpSecret = secret;
        this.totpConfirmed = false;
        this.audit = audit.touch(id.asString());
    }

    /** Ativa o 2FA após o usuário confirmar com um código válido. */
    public void confirmTotp() {
        ensureNotSuspended();
        if (totpSecret == null) {
            throw new UnprocessableEntityException("TOTP_NOT_INITIALIZED", "Setup de 2FA não iniciado");
        }
        this.totpConfirmed = true;
        this.audit = audit.touch(id.asString());
    }

    public void disableTotp() {
        this.totpSecret = null;
        this.totpConfirmed = false;
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

    public boolean isActive() { return status == AccountStatus.ACTIVE; }

    /** 2FA é exigido no login somente após confirmação do código. */
    public boolean requiresTotp() { return totpSecret != null && totpConfirmed; }

    /** Segredo gerado mas ainda não confirmado pelo usuário. */
    public boolean hasPendingTotpSetup() { return totpSecret != null && !totpConfirmed; }

    public boolean totpEnabled() { return requiresTotp(); }

    public UserId id() { return id; }
    public Email email() { return email; }
    public Password passwordHash() { return passwordHash; }
    public UserRole role() { return role; }
    public AccountStatus status() { return status; }
    public boolean emailVerified() { return emailVerified; }
    public Cpf cpf() { return cpf; }
    public LocalDate birthDate() { return birthDate; }
    public TotpSecret totpSecret() { return totpSecret; }
    public boolean totpConfirmed() { return totpConfirmed; }
    public AuditMetadata audit() { return audit; }
}
