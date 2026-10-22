package br.com.sentinelcorp.Neuro_Gen.identity.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Corpo de login. {@code totpCode} é opcional (apenas quando 2FA está ativo). */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        String totpCode) {
}
