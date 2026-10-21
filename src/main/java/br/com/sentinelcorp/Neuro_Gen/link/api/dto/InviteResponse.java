package br.com.sentinelcorp.Neuro_Gen.link.api.dto;

import br.com.sentinelcorp.Neuro_Gen.link.application.result.InviteResult;

/** Resposta de criação de convite — o token é exibido uma única vez ao Titular. */
public record InviteResponse(String linkId, String inviteToken, String inviteUrl, String expiresAt) {

    public static InviteResponse from(InviteResult r) {
        return new InviteResponse(r.linkId(), r.inviteToken(), r.inviteUrl(), r.expiresAt());
    }
}
