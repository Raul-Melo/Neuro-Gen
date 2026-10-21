package br.com.sentinelcorp.Neuro_Gen.identity.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Código TOTP de 6 dígitos para confirmar ou desativar o 2FA. */
public record TotpCodeRequest(@NotBlank String code) {
}
