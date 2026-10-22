package br.com.sentinelcorp.Neuro_Gen.shared.config;

import br.com.sentinelcorp.Neuro_Gen.shared.security.AesGcmCipher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Disponibiliza a cifra AES-256-GCM construída a partir da chave configurada. */
@Configuration
public class CryptoConfig {

    @Bean
    public AesGcmCipher aesGcmCipher(NeuroGenProperties props) {
        return AesGcmCipher.fromKey(props.getSensitiveDataKey());
    }
}
