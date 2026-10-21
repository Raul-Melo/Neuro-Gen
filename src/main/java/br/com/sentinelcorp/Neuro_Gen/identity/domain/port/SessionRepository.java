package br.com.sentinelcorp.Neuro_Gen.identity.domain.port;

import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.RefreshSession;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;

import java.util.Optional;

/** Porta de saída para persistência de refresh tokens (sessões). */
public interface SessionRepository {

    RefreshSession save(RefreshSession session);

    Optional<RefreshSession> findByTokenHash(String refreshTokenHash);

    void revokeByTokenHash(String refreshTokenHash);

    /** Revoga todas as sessões ativas de um usuário — logout global e revogação de vínculo (RF-01.4). */
    void revokeAllByUserId(UserId userId);
}
