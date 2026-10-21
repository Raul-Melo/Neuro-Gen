package br.com.sentinelcorp.Neuro_Gen.vault.domain.port;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;

/**
 * Sessão de curta duração do Cofre (auto-lock após inatividade — RF-08.6). Emitida ao abrir o Cofre
 * com a senha mestra; usada para autorizar operações subsequentes sobre as entradas.
 */
public interface VaultSessionService {

    OpenedSession open(UserId userId);

    /** Valida o token e retorna o dono; lança VaultSessionExpiredException se inválido/expirado. */
    UserId validate(String vaultSessionToken);

    void invalidate(String vaultSessionToken);

    record OpenedSession(String token, long expiresInSeconds) {
    }
}
