package br.com.sentinelcorp.Neuro_Gen.identity.domain.port;

import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.VerificationToken;

import java.util.Optional;

/** Porta de saída para tokens de redefinição de senha (TTL 15min — RF-01.7). */
public interface PasswordResetTokenRepository {

    VerificationToken save(VerificationToken token);

    Optional<VerificationToken> findByTokenHash(String tokenHash);

    VerificationToken markUsed(VerificationToken token);
}
