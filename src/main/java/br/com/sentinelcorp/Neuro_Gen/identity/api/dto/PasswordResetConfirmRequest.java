package br.com.sentinelcorp.Neuro_Gen.identity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank String resetToken,
        @NotBlank @Size(min = 8, max = 128) String newPassword) {
}
