package br.com.sentinelcorp.Neuro_Gen.identity.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(@NotBlank String refreshToken) {
}
