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
    void flywayAppliesIdentitySchemaAndSeedData() {
        MigrationInfo currentMigration =
                flyway.info().current();

        assertThat(currentMigration)
                .isNotNull();

        assertThat(currentMigration.getVersion())
                .isNotNull();

        assertThat(
                currentMigration
                        .getVersion()
                        .getVersion()
        ).isEqualTo("3");

        for (String tableName : EXPECTED_TABLES) {
            Integer tableCount =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT COUNT(*)
                            FROM information_schema.tables
                            WHERE table_schema = 'public'
                              AND table_name = ?
                            """,
                            Integer.class,
                            tableName
                    );

            assertThat(tableCount)
                    .as(
                            "Table %s should exist",
                            tableName
                    )
                    .isEqualTo(1);
        }

        Integer roleCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM roles",
                        Integer.class
                );

        assertThat(roleCount)
                .isEqualTo(6);

        Integer revokedAtColumnCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'password_reset_tokens'
                          AND column_name = 'revoked_at'
                        """,
                        Integer.class
                );

        assertThat(revokedAtColumnCount)
                .isEqualTo(1);
    }
}