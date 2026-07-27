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
            BATCH_COLUMNS = List.of(
            "id",
            "code",
            "name",
            "branch_id",
            "classroom_id",
            "course_level_id",
            "instructor_id",
            "capacity",
            "start_date",
            "end_date",
            "status",
            "created_at",
            "updated_at"
    );

    private static final List<String>
            BATCH_SCHEDULE_COLUMNS = List.of(
            "id",
            "batch_id",
            "day_of_week",
            "start_time",
            "end_time",
            "active",
            "created_at",
            "updated_at"
    );

    private static final List<String>
            BATCH_SESSION_COLUMNS = List.of(
            "id",
            "batch_id",
            "batch_schedule_id",
            "session_date",
            "start_time",
            "end_time",
            "topic",
            "status",
            "created_at",
            "updated_at"
    );

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

    private static final List<String>
            COURSE_COLUMNS = List.of(
            "id",
            "code",
            "name",
            "description",
            "active",
            "created_at",
            "updated_at"
    );

    private static final List<String>
            COURSE_LEVEL_COLUMNS = List.of(
            "id",
            "course_id",
            "code",
            "name",
            "sequence_number",
            "duration_hours",
            "description",
            "active",
            "created_at",
            "updated_at"
    );

    private static final List<String>
            INSTRUCTOR_COLUMNS = List.of(
            "id",
            "code",
            "first_name",
            "last_name",
            "email",
            "phone",
            "specialization",
            "bio",
            "active",
            "created_at",
            "updated_at"
    );

    private static final List<String>
            SEAT_RESERVATION_COLUMNS = List.of(
            "id",
            "batch_id",
            "enrollment_id",
            "status",
            "reserved_at",
            "released_at",
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

        assertThat(currentMigration).isNotNull();
        assertThat(currentMigration.getVersion())
                .isNotNull();

        assertThat(
                currentMigration
                        .getVersion()
                        .getVersion()
        ).isEqualTo("8");

        assertTableAndColumns(
                "batches",
                BATCH_COLUMNS
        );

        assertTableAndColumns(
                "batch_schedules",
                BATCH_SCHEDULE_COLUMNS
        );

        assertTableAndColumns(
                "batch_sessions",
                BATCH_SESSION_COLUMNS
        );

        assertTableAndColumns(
                "branches",
                BRANCH_COLUMNS
        );

        assertTableAndColumns(
                "classrooms",
                CLASSROOM_COLUMNS
        );

        assertTableAndColumns(
                "courses",
                COURSE_COLUMNS
        );

        assertTableAndColumns(
                "course_levels",
                COURSE_LEVEL_COLUMNS
        );

        assertTableAndColumns(
                "instructors",
                INSTRUCTOR_COLUMNS
        );

        assertTableAndColumns(
                "seat_reservations",
                SEAT_RESERVATION_COLUMNS
        );

        assertThat(countRows("batches")).isZero();
        assertThat(countRows("batch_schedules")).isZero();
        assertThat(countRows("batch_sessions")).isZero();
        assertThat(countRows("branches")).isZero();
        assertThat(countRows("classrooms")).isZero();
        assertThat(countRows("courses")).isZero();
        assertThat(countRows("course_levels")).isZero();
        assertThat(countRows("instructors")).isZero();
        assertThat(countRows("seat_reservations")).isZero();
    }

    private Long countRows(String tableName) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName,
                Long.class
        );
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