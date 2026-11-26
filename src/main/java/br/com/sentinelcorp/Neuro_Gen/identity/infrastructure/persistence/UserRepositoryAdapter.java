package br.com.sentinelcorp.Neuro_Gen.identity.infrastructure.persistence;

import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.AccountStatus;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.Cpf;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.Password;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.TotpSecret;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.User;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.UserRole;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.UserRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.AuditMetadata;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.Email;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Adaptador que traduz entre o agregado {@code User} e as entidades JPA. */
@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository userJpa;
    private final TotpSecretJpaRepository totpJpa;

    public UserRepositoryAdapter(UserJpaRepository userJpa, TotpSecretJpaRepository totpJpa) {
        this.userJpa = userJpa;
        this.totpJpa = totpJpa;
    }

    @Override
    public User save(User user) {
        UserEntity entity = userJpa.findById(user.id().value()).orElseGet(UserEntity::new);
        entity.setId(user.id().value());
        entity.setEmail(user.email().value());
        entity.setPasswordHash(user.passwordHash().argon2Hash());
        entity.setRole(user.role().name());
        entity.setStatus(user.status().name());
        entity.setEmailVerified(user.emailVerified());
        entity.setTotpEnabled(user.totpEnabled());
        entity.setCpf(user.cpf() == null ? null : user.cpf().digits());
        entity.setBirthDate(user.birthDate());
        entity.setCreatedAt(user.audit().createdAt());
        entity.setUpdatedAt(user.audit().updatedAt());
        entity.setDeletedAt(user.status() == AccountStatus.DELETED ? Instant.now() : null);
        userJpa.save(entity);
        syncTotp(user);
        return user;
    }

    private void syncTotp(User user) {
        if (user.totpSecret() != null) {
            TotpSecretEntity totp = totpJpa.findByUserId(user.id().value()).orElseGet(() -> {
                TotpSecretEntity e = new TotpSecretEntity();
                e.setId(UUID.randomUUID());
                e.setUserId(user.id().value());
                e.setCreatedAt(Instant.now());
                e.setEnabledAt(Instant.now());
                e.setSetupShown(false);
                e.setConfirmed(false);
                return e;
            });
            totp.setEncryptedSecret(user.totpSecret().encryptedBase32Secret());
            totp.setConfirmed(user.totpConfirmed());
            totpJpa.save(totp);
        } else {
            totpJpa.findByUserId(user.id().value()).ifPresent(e -> totpJpa.deleteByUserId(user.id().value()));
        }
    }

    @Override
    public Optional<User> findById(UserId id) {
        return userJpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return userJpa.findByEmail(email.value()).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return userJpa.existsByEmail(email.value());
    }

    @Override
    public void deleteById(UserId id) {
        totpJpa.findByUserId(id.value()).ifPresent(e -> totpJpa.deleteByUserId(id.value()));
        userJpa.deleteById(id.value());
    }

    private User toDomain(UserEntity e) {
        var totpRow = totpJpa.findByUserId(e.getId());
        TotpSecret secret = totpRow.map(t -> new TotpSecret(t.getEncryptedSecret())).orElse(null);
        boolean totpConfirmed = totpRow.map(TotpSecretEntity::isConfirmed).orElse(false);
        Cpf cpf = e.getCpf() == null ? null : new Cpf(e.getCpf());
        return User.reconstitute(
                new UserId(e.getId()),
                new Email(e.getEmail()),
                Password.ofHash(e.getPasswordHash()),
                UserRole.valueOf(e.getRole()),
                AccountStatus.valueOf(e.getStatus()),
                e.isEmailVerified(),
                cpf,
                e.getBirthDate(),
                secret,
                totpConfirmed,
                new AuditMetadata(e.getCreatedAt(), e.getUpdatedAt(), e.getId().toString()));
    }
}
