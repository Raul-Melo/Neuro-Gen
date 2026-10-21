package br.com.sentinelcorp.Neuro_Gen.vault.infrastructure.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Datasource ISOLADO do Vault (ADR-005 / RNF-SEC.4). Pool, EntityManager, TransactionManager e
 * Flyway próprios, apontando para o schema {@code ng_vault}. Em produção, instância PostgreSQL e
 * credenciais fisicamente separadas; aqui o isolamento é lógico (schema + datasource dedicados).
 *
 * <p>Fica no pacote de infraestrutura do Vault — nenhum outro módulo o referencia.</p>
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "br.com.sentinelcorp.Neuro_Gen.vault.infrastructure.persistence",
        entityManagerFactoryRef = "vaultEntityManagerFactory",
        transactionManagerRef = "vaultTransactionManager")
public class VaultDataSourceConfig {

    @Bean
    @ConfigurationProperties("vault.datasource")
    public DataSourceProperties vaultDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("vault.datasource.hikari")
    public DataSource vaultDataSource(
            @Qualifier("vaultDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    /**
     * Migra o schema ng_vault no bootstrap. NÃO é um bean do tipo {@link Flyway} de propósito: expor
     * um bean {@code Flyway} desativaria o Flyway automático do Core (@ConditionalOnMissingBean).
     */
    @Bean
    public VaultFlywayInitializer vaultFlywayInitializer(@Qualifier("vaultDataSource") DataSource dataSource) {
        return new VaultFlywayInitializer(dataSource);
    }

    /** Wrapper que executa a migração do Vault na inicialização. */
    public static class VaultFlywayInitializer {
        public VaultFlywayInitializer(DataSource dataSource) {
            Flyway.configure()
                    .dataSource(dataSource)
                    .schemas("ng_vault")
                    .defaultSchema("ng_vault")
                    .createSchemas(true)
                    .table("flyway_schema_history_vault")
                    .locations("classpath:db/vault/migration")
                    .load()
                    .migrate();
        }
    }

    @Bean
    @DependsOn("vaultFlywayInitializer")
    public LocalContainerEntityManagerFactoryBean vaultEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("vaultDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("br.com.sentinelcorp.Neuro_Gen.vault.infrastructure.persistence")
                .persistenceUnit("vault")
                .build();
    }

    @Bean
    public PlatformTransactionManager vaultTransactionManager(
            @Qualifier("vaultEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
