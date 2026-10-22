package br.com.sentinelcorp.Neuro_Gen.shared.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A chave AES-256 vem de variável de ambiente em produção. Provedores de nuvem e ferramentas
 * ({@code openssl rand -base64 32}, {@code generateValue} do Render) geram Base64; o ambiente de
 * desenvolvimento usa hexadecimal. Ambos os formatos devem ser aceitos.
 */
class AesGcmCipherTest {

    private static final String HEX_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String BASE64_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void aceitaChaveHexadecimalDe64Caracteres() {
        AesGcmCipher cipher = AesGcmCipher.fromKey(HEX_KEY);
        assertThat(cipher.decryptFromBase64(cipher.encryptToBase64("humor: 4"))).isEqualTo("humor: 4");
    }

    @Test
    void aceitaChaveBase64De32Bytes() {
        AesGcmCipher cipher = AesGcmCipher.fromKey(BASE64_KEY);
        assertThat(cipher.decryptFromBase64(cipher.encryptToBase64("diário"))).isEqualTo("diário");
    }

    @Test
    void ignoraEspacosEQuebrasDeLinhaNasPontas() {
        AesGcmCipher cipher = AesGcmCipher.fromKey("  " + BASE64_KEY + "\n");
        assertThat(cipher.decryptFromBase64(cipher.encryptToBase64("ok"))).isEqualTo("ok");
    }

    @Test
    void hexEBase64DaMesmaChaveSaoIntercambiaveis() {
        byte[] raw = new byte[32];
        for (int i = 0; i < raw.length; i++) {
            raw[i] = (byte) i;
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : raw) {
            hex.append(String.format("%02x", b));
        }
        String cifrado = AesGcmCipher.fromKey(hex.toString()).encryptToBase64("segredo");
        String decifrado = AesGcmCipher.fromKey(Base64.getEncoder().encodeToString(raw)).decryptFromBase64(cifrado);
        assertThat(decifrado).isEqualTo("segredo");
    }

    @Test
    void rejeitaChaveBase64ComTamanhoErrado() {
        String chave128bits = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> AesGcmCipher.fromKey(chave128bits))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "não é base64 nem hex!"})
    void rejeitaChaveAusenteOuInvalidaComMensagemClara(String chave) {
        assertThatThrownBy(() -> AesGcmCipher.fromKey(chave))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chave");
    }

    @Test
    void mesmoTextoGeraCifrasDiferentesPorCausaDoIvAleatorio() {
        AesGcmCipher cipher = AesGcmCipher.fromKey(BASE64_KEY);
        assertThat(cipher.encryptToBase64("x")).isNotEqualTo(cipher.encryptToBase64("x"));
    }
}
