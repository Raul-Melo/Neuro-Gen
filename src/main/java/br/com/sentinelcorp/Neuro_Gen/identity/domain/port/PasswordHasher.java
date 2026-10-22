package br.com.sentinelcorp.Neuro_Gen.identity.domain.port;

import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.Password;

/** Porta de saída: deriva e verifica hashes Argon2id. Implementada na infra (Spring Security). */
public interface PasswordHasher {

    Password hash(String rawPassword);

    boolean matches(String rawPassword, Password password);
}
