package br.com.sentinelcorp.Neuro_Gen.identity.domain.port;

import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.User;

/** Porta de saída: emite o access token JWT de curta duração a partir do usuário autenticado. */
public interface TokenIssuer {

    IssuedAccessToken issueAccessToken(User user);

    record IssuedAccessToken(String token, long expiresInSeconds) {
    }
}
