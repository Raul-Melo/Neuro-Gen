package br.com.sentinelcorp.Neuro_Gen.identity.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Solicitação de redefinição de senha (envia e-mail com token). */
public record PasswordResetRequestRequest(@NotBlank @Email String email) {
}
