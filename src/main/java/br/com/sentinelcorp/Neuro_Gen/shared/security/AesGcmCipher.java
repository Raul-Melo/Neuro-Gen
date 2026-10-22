package br.com.sentinelcorp.Neuro_Gen.shared.security;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifra AES-256-GCM (RNF-SEC.2). Usada para cifrar o segredo TOTP e as colunas de dados sensíveis
 * de saúde (schema {@code ng_sensitive} — ADR-006). O formato serializado é {@code base64(iv || ciphertext+tag)}.
 *
 * <p>A chave (32 bytes) vem de configuração externa (cofre de segredos em produção — RNF-SEC.13).</p>
 */
public final class AesGcmCipher {

    private static final int IV_LENGTH = 12;      // 96 bits — recomendado para GCM
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec key;

    public AesGcmCipher(byte[] key) {
        if (key == null || key.length != 32) {
            throw new IllegalArgumentException("A chave AES-256 deve ter exatamente 32 bytes");
        }
        this.key = new SecretKeySpec(key, "AES");
    }

    /**
     * Constrói a partir de uma chave de 32 bytes em <b>hexadecimal</b> (64 caracteres, usado em dev)
     * ou em <b>Base64</b> (formato de geradores de segredo, ex.: {@code openssl rand -base64 32}).
     */
    public static AesGcmCipher fromKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("A chave AES-256 de dados sensíveis não foi configurada");
        }
        String trimmed = key.strip();
        if (trimmed.length() == 64 && trimmed.chars().allMatch(c -> Character.digit(c, 16) >= 0)) {
            return fromHexKey(trimmed);
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(trimmed);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "A chave AES-256 deve estar em hexadecimal (64 caracteres) ou Base64", ex);
        }
        return new AesGcmCipher(decoded);
    }

    /** Constrói a partir de uma chave em hexadecimal (64 caracteres). */
    public static AesGcmCipher fromHexKey(String hexKey) {
        return new AesGcmCipher(hexToBytes(hexKey));
    }

    public String encryptToBase64(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] out = ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array();
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao cifrar", ex);
        }
    }

    public String decryptFromBase64(String base64) {
        if (base64 == null) {
            return null;
        }
        try {
            byte[] in = Base64.getDecoder().decode(base64);
            ByteBuffer buffer = ByteBuffer.wrap(in);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ciphertext), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao decifrar", ex);
        }
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
