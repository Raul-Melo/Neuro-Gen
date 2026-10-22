package br.com.sentinelcorp.Neuro_Gen.vault.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;

import java.util.Base64;
import java.util.Map;

/**
 * Conteúdo cifrado AES-256-GCM cuja chave é derivada via Argon2id <b>no cliente</b> — o servidor
 * NUNCA vê texto claro nem a chave (zero-knowledge, RNF-SEC.3). Transporta apenas bytes.
 *
 * <p>Formato serializado (compacto): {@code base64(ciphertext).base64(iv).base64(salt)}.</p>
 */
public record EncryptedPayload(byte[] ciphertext, byte[] iv, byte[] salt) {

    public EncryptedPayload {
        if (ciphertext == null || ciphertext.length == 0 || iv == null || iv.length == 0
                || salt == null || salt.length == 0) {
            throw new ValidationException(Map.of("payload", "ciphertext, iv e salt são obrigatórios"));
        }
    }

    public String toBase64Json() {
        Base64.Encoder enc = Base64.getEncoder();
        return enc.encodeToString(ciphertext) + "." + enc.encodeToString(iv) + "." + enc.encodeToString(salt);
    }

    public static EncryptedPayload fromBase64Json(String json) {
        if (json == null) {
            throw new ValidationException(Map.of("payload", "payload ausente"));
        }
        String[] parts = json.split("\\.");
        if (parts.length != 3) {
            throw new ValidationException(Map.of("payload", "formato inválido (esperado ct.iv.salt)"));
        }
        Base64.Decoder dec = Base64.getDecoder();
        try {
            return new EncryptedPayload(dec.decode(parts[0]), dec.decode(parts[1]), dec.decode(parts[2]));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(Map.of("payload", "base64 inválido"));
        }
    }
}
