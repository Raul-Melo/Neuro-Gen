package br.com.sentinelcorp.Neuro_Gen.identity.application.result;

/**
 * Dados do setup de 2FA, exibidos uma única vez: URI otpauth (para QR code) e o segredo em base32.
 * Só se tornam efetivos após confirmação com um código válido (ConfirmTotp).
 */
public record TotpSetupResult(String otpAuthUri, String base32Secret) {
}
