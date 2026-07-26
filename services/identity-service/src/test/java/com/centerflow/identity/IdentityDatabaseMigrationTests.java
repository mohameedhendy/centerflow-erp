package com.centerflow.identity;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class IdentityDatabaseMigrationTests {

    private static final List<String> EXPECTED_TABLES = List.of(
            "users",
            "roles",
            "permissions",
            "user_roles",
            "role_permissions",
            "refresh_token_sessions",
            "password_reset_tokens"
    );

    private final Flyway flyway;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    IdentityDatabaseMigrationTests(
            Flyway flyway,
            JdbcTemplate jdbcTemplate
    ) {
        this.flyway = flyway;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void flywayAppliesInitialIdentitySchema() {
        MigrationInfo currentMigration = flyway.info().current();

        assertThat(currentMigration).isNotNull();
        assertThat(currentMigration.getVersion()).isNotNull();
        assertThat(currentMigration.getVersion().getVersion()).isEqualTo("1");

        for (String tableName : EXPECTED_TABLES) {
            Long rowCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + tableName,
                    Long.class
            );

            assertThat(rowCount)
                    .as("Table %s should exist and initially be empty", tableName)
                    .isZero();
        }
    }
}