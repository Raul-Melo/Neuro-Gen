package br.com.sentinelcorp.Neuro_Gen.identity.api.dto;

import br.com.sentinelcorp.Neuro_Gen.identity.application.result.AuthResult;

/** Resposta de autenticação — tokens Bearer. */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType) {

    public static TokenResponse from(AuthResult result) {
        return new TokenResponse(result.accessToken(), result.refreshToken(), result.expiresIn(), "Bearer");
    }
}
