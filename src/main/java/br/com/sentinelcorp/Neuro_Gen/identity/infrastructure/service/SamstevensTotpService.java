package br.com.sentinelcorp.Neuro_Gen.identity.infrastructure.service;

import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.TotpSecret;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.TotpService;
import br.com.sentinelcorp.Neuro_Gen.shared.security.AesGcmCipher;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.stereotype.Component;

/**
 * Implementação de 2FA/TOTP (lib samstevens). O segredo base32 é cifrado em repouso (AES-256-GCM)
 * e só é decifrado em memória no momento da verificação do código (janela de 30s).
 */
@Component
public class SamstevensTotpService implements TotpService {

    private static final String ISSUER = "Neuro-Gen";

    private final AesGcmCipher cipher;
    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final CodeVerifier codeVerifier =
            new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());

    public SamstevensTotpService(AesGcmCipher cipher) {
        this.cipher = cipher;
    }

    @Override
    public GeneratedTotp generate(String userEmail) {
        String base32 = secretGenerator.generate();
        String encrypted = cipher.encryptToBase64(base32);
        QrData data = new QrData.Builder()
                .label(userEmail)
                .secret(base32)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        return new GeneratedTotp(new TotpSecret(encrypted), base32, data.getUri());
    }

    @Override
    public boolean verify(TotpSecret secret, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String base32 = cipher.decryptFromBase64(secret.encryptedBase32Secret());
        return codeVerifier.isValidCode(base32, code);
    }
}
