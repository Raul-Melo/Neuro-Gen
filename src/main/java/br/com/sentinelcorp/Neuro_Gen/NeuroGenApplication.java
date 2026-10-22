package br.com.sentinelcorp.Neuro_Gen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Ponto de entrada do monólito modular Neuro-Gen.
 *
 * <p>Os datasources (core + vault) e os repositórios JPA são configurados explicitamente em
 * {@code shared.config.CoreDataSourceConfig} e {@code vault.infrastructure.config.VaultDataSourceConfig}.</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class NeuroGenApplication {

	public static void main(String[] args) {
		SpringApplication.run(NeuroGenApplication.class, args);
	}

}
