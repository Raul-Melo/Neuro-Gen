package br.com.sentinelcorp.Neuro_Gen.shared.security;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

/**
 * Acesso utilitário ao usuário autenticado no contexto de segurança corrente.
 * Centraliza a leitura dos claims do JWT para os controllers.
 */
public final class CurrentUser {

    public static final String ROLE_CLAIM = "role";
    public static final String EMAIL_VERIFIED_CLAIM = "email_verified";

    private CurrentUser() {
    }

    public static Optional<AuthenticatedUser> get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        String role = jwt.getClaimAsString(ROLE_CLAIM);
        Boolean verified = jwt.getClaim(EMAIL_VERIFIED_CLAIM);
        return Optional.of(new AuthenticatedUser(
                UserId.of(jwt.getSubject()), role, Boolean.TRUE.equals(verified)));
    }

    public static AuthenticatedUser require() {
        return get().orElseThrow(() -> new IllegalStateException("Nenhum usuário autenticado no contexto"));
    }

    public static UserId requireId() {
        return require().userId();
    }
}
