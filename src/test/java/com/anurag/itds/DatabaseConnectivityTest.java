package com.anurag.itds;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;

/**
 * Phase 1 — Database connectivity & JDBC wiring verification.
 *
 * Uses the lightweight {@code @JdbcTest} slice with an auto-configured embedded
 * (H2) database, so it is fast and NEVER connects to the real MySQL database.
 * It proves that Spring Boot correctly auto-configures a working
 * {@link DataSource} and that a JDBC query can execute against it.
 */
@JdbcTest
@AutoConfigureTestDatabase
class DatabaseConnectivityTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("DataSource is configured and a JDBC query executes successfully")
    void dataSourceIsUsable() throws Exception {
        assertThat(dataSource).isNotNull();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT 1")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }
}
