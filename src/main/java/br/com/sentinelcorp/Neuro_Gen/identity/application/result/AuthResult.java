package br.com.sentinelcorp.Neuro_Gen.identity.application.result;

/** Resultado de autenticação: access token JWT curto + refresh token opaco + dados do usuário. */
public record AuthResult(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserResult user) {
}
