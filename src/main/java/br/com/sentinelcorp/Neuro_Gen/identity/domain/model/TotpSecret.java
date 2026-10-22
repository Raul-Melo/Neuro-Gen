package br.com.sentinelcorp.Neuro_Gen.identity.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;

import java.util.Map;

/**
 * Value Object para o segredo TOTP (2FA — RF-01.9). Armazena o segredo <b>cifrado</b> em repouso
 * (AES-256-GCM). A verificação do código e a geração do URI otpauth são delegadas à porta
 * {@code TotpService} (infra), pois exigem decifração e lógica de tempo.
 */
public record TotpSecret(String encryptedBase32Secret) {

    public TotpSecret {
        if (encryptedBase32Secret == null || encryptedBase32Secret.isBlank()) {
            throw new ValidationException(Map.of("totpSecret", "segredo inválido"));
        }
    }
}
