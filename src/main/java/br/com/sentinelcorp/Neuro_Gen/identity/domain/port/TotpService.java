package br.com.sentinelcorp.Neuro_Gen.identity.domain.port;

import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.TotpSecret;

/** Porta de saída para 2FA/TOTP: geração de segredo, URI otpauth e verificação de código. */
public interface TotpService {

    /** Gera um novo segredo (cifrado em repouso) + metadados para exibição no setup (QR code). */
    GeneratedTotp generate(String userEmail);

    /** @return true se o código informado é válido para o segredo na janela de tempo atual. */
    boolean verify(TotpSecret secret, String code);

    /** Segredo recém-gerado. {@code base32Plain}/{@code otpAuthUri} são exibidos uma única vez no setup. */
    record GeneratedTotp(TotpSecret secret, String base32Plain, String otpAuthUri) {
    }
}
