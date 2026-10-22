package br.com.sentinelcorp.Neuro_Gen.identity.infrastructure.service;

import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.Password;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Implementação da porta {@code PasswordHasher} usando o Argon2id configurado (RNF-SEC.5). */
@Component
public class Argon2PasswordHasher implements PasswordHasher {

    private final PasswordEncoder encoder;

    public Argon2PasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public Password hash(String rawPassword) {
        return Password.ofHash(encoder.encode(rawPassword));
    }

    @Override
    public boolean matches(String rawPassword, Password password) {
        return encoder.matches(rawPassword, password.argon2Hash());
    }
}
