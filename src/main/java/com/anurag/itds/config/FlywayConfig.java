package com.anurag.itds.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Flyway migration wiring.
 *
 * <p><strong>Why this class exists:</strong> Spring Boot 4.x removed the Flyway
 * auto-configuration that Spring Boot 3.x shipped in
 * {@code spring-boot-autoconfigure} ({@code FlywayAutoConfiguration} no longer
 * exists in Boot 4). Merely adding {@code flyway-core}/{@code flyway-mysql} to
 * the classpath therefore does <em>not</em> run any migration — which is exactly
 * why the application previously started against an empty database and Hibernate
 * {@code ddl-auto=validate} failed with {@code missing table [community_reports]}.
 * Declaring the {@link Flyway} bean here restores migration execution on
 * startup.</p>
 *
 * <p>The bean is gated on {@code safewalk.flyway-enabled} (default {@code true}) so
 * the test profile can disable it — tests use Hibernate {@code create-drop}
 * against in-memory H2 and must never run the MySQL-flavoured migrations.</p>
 *
 * <p>Migration order vs Hibernate: the Flyway bean is registered as database
 * initialization, and JPA's {@code EntityManagerFactory} is annotated
 * {@code @DependsOnDatabaseInitialization}, so migrations always run
 * <em>before</em> Hibernate schema validation.</p>
 */
@Configuration
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Bean
    @ConditionalOnProperty(name = "safewalk.flyway-enabled", havingValue = "true", matchIfMissing = true)
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
        // Eagerly migrate at startup so the schema exists before Hibernate validates it.
        flyway.migrate();
        log.info("Flyway migrations applied (locations=classpath:db/migration).");
        return flyway;
    }
}
