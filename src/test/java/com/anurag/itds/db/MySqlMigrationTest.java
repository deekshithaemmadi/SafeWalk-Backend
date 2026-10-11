package com.anurag.itds.db;

import com.anurag.itds.entity.User;
import com.anurag.itds.repository.UserRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the REAL production Flyway migrations against a real MySQL 8.4 server and
 * lets Hibernate validate every entity against the resulting schema
 * ({@code ddl-auto=validate}), exactly as production does. Requires Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "safewalk.datasets.import-on-startup=true"
})
class MySqlMigrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Autowired
    private UserRepository userRepository;

    @Test
    void allMigrationsApplySuccessfullyAndSchemaValidates() {
        List<String> versions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = 1 AND version IS NOT NULL ORDER BY installed_rank",
                String.class);
        assertThat(versions).contains("1", "2", "3");
        assertThat(flyway.info().pending()).isEmpty();

        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()", String.class);
        assertThat(tables).extracting(String::toLowerCase).contains(
                "users", "emergency_contacts", "community_reports", "help_locations", "sos_events", "trips");
    }

    @Test
    void migrationsAreIdempotentOnRerun() {
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test
    void entitiesPersistAgainstMigratedSchema() {
        User saved = userRepository.save(new User("Migration Test", "migration@test.local", "9000000001", "hash"));
        assertThat(saved.getId()).isNotNull();
        assertThat(userRepository.findByEmailIgnoreCase("MIGRATION@test.local")).isPresent();
    }
    @Test
    void bundledDatasetsImportIntoMigratedSchema() {
        Long locations = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM help_locations", Long.class);
        Long stats = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM women_crime_statistics", Long.class);
        Integer longestSectors = jdbcTemplate.queryForObject(
                "SELECT MAX(CHAR_LENGTH(support_sectors)) FROM help_locations", Integer.class);
        assertThat(locations).isGreaterThan(1400);
        assertThat(stats).isEqualTo(126);
        assertThat(longestSectors).isGreaterThan(500);
    }
}
