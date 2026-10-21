package br.com.sentinelcorp.Neuro_Gen.identity.domain.port;

import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.VerificationToken;

import java.util.Optional;

/** Porta de saída para tokens de verificação de e-mail (TTL 24h). */
public interface EmailVerificationTokenRepository {

    VerificationToken save(VerificationToken token);

    Optional<VerificationToken> findByTokenHash(String tokenHash);

    VerificationToken markUsed(VerificationToken token);
}
