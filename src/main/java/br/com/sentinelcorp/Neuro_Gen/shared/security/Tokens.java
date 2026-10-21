package br.com.sentinelcorp.Neuro_Gen.shared.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Utilitários puros (JDK) para tokens opacos: geração segura e hashing SHA-256. Tokens de convite,
 * verificação de e-mail, reset e refresh são armazenados apenas como hash (nunca em texto claro).
 */
public final class Tokens {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private Tokens() {
    }

    /** Gera um token opaco URL-safe com {@code bytes} de entropia. */
    public static String random(int bytes) {
        byte[] buffer = new byte[bytes];
        RANDOM.nextBytes(buffer);
        return URL_ENCODER.encodeToString(buffer);
    }

    public static String random() {
        return random(32);
    }

    /** SHA-256 em hexadecimal — usado para persistir hashes de tokens. */
    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 indisponível", ex);
        }
    }
}
