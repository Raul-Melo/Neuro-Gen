package br.com.sentinelcorp.Neuro_Gen.identity.infrastructure.persistence;

import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.RefreshSession;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.SessionRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/** Adaptador de persistência de refresh tokens (sessões). */
@Repository
public class SessionRepositoryAdapter implements SessionRepository {

    private final SessionJpaRepository jpa;

    public SessionRepositoryAdapter(SessionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public RefreshSession save(RefreshSession session) {
        SessionEntity e = new SessionEntity();
        e.setId(session.id());
        e.setUserId(session.userId().value());
        e.setRefreshTokenHash(session.refreshTokenHash());
        e.setExpiresAt(session.expiresAt());
        e.setDeviceInfo(session.deviceInfo());
        e.setCreatedAt(session.createdAt());
        e.setRevokedAt(session.revokedAt());
        jpa.save(e);
        return session;
    }

    @Override
    public Optional<RefreshSession> findByTokenHash(String refreshTokenHash) {
        return jpa.findByRefreshTokenHash(refreshTokenHash).map(this::toDomain);
    }

    @Override
    public void revokeByTokenHash(String refreshTokenHash) {
        jpa.revokeByTokenHash(refreshTokenHash, Instant.now());
    }

    @Override
    public void revokeAllByUserId(UserId userId) {
        jpa.revokeAllByUserId(userId.value(), Instant.now());
    }

    private RefreshSession toDomain(SessionEntity e) {
        return new RefreshSession(e.getId(), new UserId(e.getUserId()), e.getRefreshTokenHash(),
                e.getExpiresAt(), e.getDeviceInfo(), e.getCreatedAt(), e.getRevokedAt());
    }
}
