package br.com.sentinelcorp.Neuro_Gen.identity.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Corpo de cadastro de conta (RF-01.1). CPF aceito com ou sem máscara (000.000.000-00). */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank @Pattern(regexp = "TITULAR|APOIO", message = "deve ser TITULAR ou APOIO") String role,
        @NotBlank @Pattern(regexp = "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\d{11}",
                message = "CPF inválido — use 000.000.000-00 ou 00000000000") String cpf,
        @NotNull @Past(message = "Data de nascimento deve ser no passado") LocalDate birthDate) {
}
