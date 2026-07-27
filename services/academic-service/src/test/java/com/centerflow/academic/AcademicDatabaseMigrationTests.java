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

    private static final List<String>
            BRANCH_COLUMNS = List.of(
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

    private static final List<String>
            CLASSROOM_COLUMNS = List.of(
            "id",
            "branch_id",
            "code",
            "name",
            "capacity",
            "floor",
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
    void flywayCreatesAcademicSchema() {
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
        ).isEqualTo("2");

        assertTableAndColumns(
                "branches",
                BRANCH_COLUMNS
        );

        assertTableAndColumns(
                "classrooms",
                CLASSROOM_COLUMNS
        );

        Long branchCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM branches",
                        Long.class
                );

        Long classroomCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM classrooms",
                        Long.class
                );

        assertThat(branchCount).isZero();
        assertThat(classroomCount).isZero();
    }

    private void assertTableAndColumns(
            String tableName,
            List<String> expectedColumns
    ) {
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
                .as("Table %s should exist", tableName)
                .isEqualTo(1);

        for (String columnName : expectedColumns) {
            Integer columnCount =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT COUNT(*)
                            FROM information_schema.columns
                            WHERE table_schema = 'public'
                              AND table_name = ?
                              AND column_name = ?
                            """,
                            Integer.class,
                            tableName,
                            columnName
                    );

            assertThat(columnCount)
                    .as(
                            "Column %s should exist in %s",
                            columnName,
                            tableName
                    )
                    .isEqualTo(1);
        }
    }
}