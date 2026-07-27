package com.centerflow.academic;

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
class AcademicDatabaseMigrationTests {

    private static final List<String> EXPECTED_COLUMNS =
            List.of(
                    "id",
                    "code",
                    "name",
                    "phone",
                    "email",
                    "address",
                    "city",
                    "active",
                    "created_at",
                    "updated_at"
            );

    private final Flyway flyway;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    AcademicDatabaseMigrationTests(
            Flyway flyway,
            JdbcTemplate jdbcTemplate
    ) {
        this.flyway = flyway;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void flywayCreatesInitialAcademicSchema() {
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
        ).isEqualTo("1");

        Integer tableCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name = 'branches'
                        """,
                        Integer.class
                );

        assertThat(tableCount)
                .isEqualTo(1);

        for (String columnName : EXPECTED_COLUMNS) {
            Integer columnCount =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT COUNT(*)
                            FROM information_schema.columns
                            WHERE table_schema = 'public'
                              AND table_name = 'branches'
                              AND column_name = ?
                            """,
                            Integer.class,
                            columnName
                    );

            assertThat(columnCount)
                    .as(
                            "Column %s should exist in branches",
                            columnName
                    )
                    .isEqualTo(1);
        }

        Long branchCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM branches",
                        Long.class
                );

        assertThat(branchCount)
                .isZero();
    }
}