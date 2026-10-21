package br.com.sentinelcorp.Neuro_Gen.identity.infrastructure.persistence;

import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.VerificationToken;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.EmailVerificationTokenRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class EmailVerificationTokenRepositoryAdapter implements EmailVerificationTokenRepository {

    private final EmailVerificationJpaRepository jpa;

    public EmailVerificationTokenRepositoryAdapter(EmailVerificationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public VerificationToken save(VerificationToken token) {
        EmailVerificationEntity e = jpa.findById(token.id()).orElseGet(EmailVerificationEntity::new);
        e.setId(token.id());
        e.setUserId(token.userId().value());
        e.setTokenHash(token.tokenHash());
        e.setExpiresAt(token.expiresAt());
        e.setUsedAt(token.usedAt());
        e.setCreatedAt(token.createdAt());
        jpa.save(e);
        return token;
    }

    @Override
    public Optional<VerificationToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public VerificationToken markUsed(VerificationToken token) {
        VerificationToken used = new VerificationToken(token.id(), token.userId(), token.tokenHash(),
                token.expiresAt(), Instant.now(), token.createdAt());
        return save(used);
    }

    private VerificationToken toDomain(EmailVerificationEntity e) {
        return new VerificationToken(e.getId(), new UserId(e.getUserId()), e.getTokenHash(),
                e.getExpiresAt(), e.getUsedAt(), e.getCreatedAt());
    }
}
