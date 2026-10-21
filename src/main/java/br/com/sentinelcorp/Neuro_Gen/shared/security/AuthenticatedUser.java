package br.com.sentinelcorp.Neuro_Gen.shared.security;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;

/**
 * Representa o usuário autenticado extraído do JWT (claim {@code sub} = userId, claim {@code role}).
 * Disponibilizado aos controllers via {@code SecurityContext}.
 */
public record AuthenticatedUser(UserId userId, String role, boolean emailVerified) {

    public boolean isTitular() {
        return "TITULAR".equals(role);
    }

    public boolean isApoio() {
        return "APOIO".equals(role);
    }
}
