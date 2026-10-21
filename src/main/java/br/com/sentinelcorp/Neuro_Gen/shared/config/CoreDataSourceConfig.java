package br.com.sentinelcorp.Neuro_Gen.shared.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Datasource PRIMÁRIO do Core (schemas {@code ng_identity}, {@code ng_core}, {@code ng_sensitive}).
 * Os repositórios Spring Data de todos os módulos de feature são escaneados aqui; o Vault é
 * deliberadamente excluído (tem datasource próprio — {@link VaultDataSourceConfig}).
 */
@Configuration
@EnableJpaRepositories(
        basePackages = {
                "br.com.sentinelcorp.Neuro_Gen.identity.infrastructure.persistence",
                "br.com.sentinelcorp.Neuro_Gen.link.infrastructure.persistence",
                "br.com.sentinelcorp.Neuro_Gen.agenda.infrastructure.persistence",
                "br.com.sentinelcorp.Neuro_Gen.focus.infrastructure.persistence",
                "br.com.sentinelcorp.Neuro_Gen.emotional.infrastructure.persistence"
        },
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager")
public class CoreDataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties coreDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource coreDataSource(
            @Qualifier("coreDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("coreDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages(
                        "br.com.sentinelcorp.Neuro_Gen.identity.infrastructure.persistence",
                        "br.com.sentinelcorp.Neuro_Gen.link.infrastructure.persistence",
                        "br.com.sentinelcorp.Neuro_Gen.agenda.infrastructure.persistence",
                        "br.com.sentinelcorp.Neuro_Gen.focus.infrastructure.persistence",
                        "br.com.sentinelcorp.Neuro_Gen.emotional.infrastructure.persistence")
                .persistenceUnit("core")
                .build();
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
