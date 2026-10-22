package br.com.sentinelcorp.Neuro_Gen.link.application.result;

/** Resultado da criação de convite — inclui o token em texto claro (exibido uma vez ao Titular). */
public record InviteResult(
        String linkId,
        String inviteToken,
        String inviteUrl,
        String expiresAt) {
}
