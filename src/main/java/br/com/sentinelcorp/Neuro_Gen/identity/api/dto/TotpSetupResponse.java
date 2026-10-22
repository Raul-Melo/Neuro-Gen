package br.com.sentinelcorp.Neuro_Gen.identity.api.dto;

import br.com.sentinelcorp.Neuro_Gen.identity.application.result.TotpSetupResult;

/** Dados de setup 2FA exibidos uma única vez (QR + segredo). */
public record TotpSetupResponse(String otpAuthUri, String base32Secret) {

    public static TotpSetupResponse from(TotpSetupResult r) {
        return new TotpSetupResponse(r.otpAuthUri(), r.base32Secret());
    }
}
