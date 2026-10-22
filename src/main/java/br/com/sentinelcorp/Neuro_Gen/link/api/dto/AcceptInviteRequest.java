package br.com.sentinelcorp.Neuro_Gen.link.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AcceptInviteRequest(@NotBlank String inviteToken) {
}
