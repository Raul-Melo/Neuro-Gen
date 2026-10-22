package br.com.sentinelcorp.Neuro_Gen.vault.infrastructure.service;

import br.com.sentinelcorp.Neuro_Gen.shared.config.VaultProperties;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import br.com.sentinelcorp.Neuro_Gen.shared.security.Tokens;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.exception.VaultExceptions.VaultSessionExpiredException;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.port.VaultSessionService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sessão do Cofre em memória com auto-lock por inatividade (RF-08.6). Em produção, usaria um store
 * com TTL (ex.: Redis) e token assinado; aqui, mantém o escopo do MVP e o isolamento do serviço.
 */
@Component
public class InMemoryVaultSessionService implements VaultSessionService {

    private record Session(UUID userId, Instant expiresAt) {
    }

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final long ttlSeconds;

    public InMemoryVaultSessionService(VaultProperties props) {
        this.ttlSeconds = props.getSession().getTtlSeconds();
    }

    @Override
    public OpenedSession open(UserId userId) {
        String token = Tokens.random(32);
        sessions.put(token, new Session(userId.value(), Instant.now().plusSeconds(ttlSeconds)));
        return new OpenedSession(token, ttlSeconds);
    }

    @Override
    public UserId validate(String vaultSessionToken) {
        Session session = vaultSessionToken == null ? null : sessions.get(vaultSessionToken);
        if (session == null || Instant.now().isAfter(session.expiresAt())) {
            if (vaultSessionToken != null) {
                sessions.remove(vaultSessionToken);
            }
            throw new VaultSessionExpiredException();
        }
        // Renova a janela de inatividade a cada uso.
        sessions.put(vaultSessionToken, new Session(session.userId(), Instant.now().plusSeconds(ttlSeconds)));
        return new UserId(session.userId());
    }

    @Override
    public void invalidate(String vaultSessionToken) {
        if (vaultSessionToken != null) {
            sessions.remove(vaultSessionToken);
        }
    }
}
