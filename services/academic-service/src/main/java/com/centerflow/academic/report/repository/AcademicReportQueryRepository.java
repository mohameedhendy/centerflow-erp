package com.centerflow.academic.report.repository;

import com.centerflow.academic.batch.domain.BatchStatus;
import com.centerflow.academic.report.api.AcademicOverviewResponse;
import com.centerflow.academic.report.api.AttendanceSummaryResponse;
import com.centerflow.academic.report.api.BatchAcademicReportResponse;
import com.centerflow.academic.report.api.BatchStatusSummaryResponse;
import com.centerflow.academic.report.api.SessionStatusSummaryResponse;
import com.centerflow.academic.report.api.StudentAttendanceReportResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AcademicReportQueryRepository {

    private static final int ATTENDANCE_RATE_SCALE = 2;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AcademicReportQueryRepository(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AcademicOverviewResponse findOverview(
            UUID branchId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        BatchStatusSummaryResponse batchSummary =
                findBatchSummary(
                        branchId,
                        fromDate,
                        toDate
                );

        SessionStatusSummaryResponse sessionSummary =
                findSessionSummary(
                        branchId,
                        null,
                        fromDate,
                        toDate
                );

        long reservedSeats =
                findCurrentReservedSeats(
                        branchId,
                        null
                );

        AttendanceSummaryResponse attendanceSummary =
                findAttendanceSummary(
                        branchId,
                        null,
                        null,
                        fromDate,
                        toDate
                );

        return new AcademicOverviewResponse(
                branchId,
                fromDate,
                toDate,
                batchSummary,
                sessionSummary,
                reservedSeats,
                attendanceSummary
        );
    }

    public Optional<BatchAcademicReportResponse>
    findBatchReport(
            UUID batchId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        MapSqlParameterSource parameters =
                new MapSqlParameterSource()
                        .addValue("batchId", batchId);

        List<BatchDetails> batches =
                jdbcTemplate.query(
                        """
                        SELECT
                            batch.id AS batch_id,
                            batch.code AS batch_code,
                            batch.name AS batch_name,
                            batch.status AS batch_status,
                            batch.capacity AS batch_capacity,
                            batch.start_date AS batch_start_date,
                            batch.end_date AS batch_end_date,

                            branch.id AS branch_id,
                            branch.code AS branch_code,
                            branch.name AS branch_name,

                            course.id AS course_id,
                            course.code AS course_code,
                            course.name AS course_name,

                            course_level.id AS course_level_id,
                            course_level.code AS course_level_code,
                            course_level.name AS course_level_name,

                            instructor.id AS instructor_id,
                            CONCAT(
                                instructor.first_name,
                                CONCAT(
                                    ' ',
                                    instructor.last_name
                                )
                            ) AS instructor_name
                        FROM batches batch
                        JOIN branches branch
                          ON branch.id = batch.branch_id
                        JOIN course_levels course_level
                          ON course_level.id =
                             batch.course_level_id
                        JOIN courses course
                          ON course.id =
                             course_level.course_id
                        JOIN instructors instructor
                          ON instructor.id =
                             batch.instructor_id
                        WHERE batch.id = :batchId
                        """,
                        parameters,
                        (resultSet, rowNumber) ->
                                new BatchDetails(
                                        readUuid(
                                                resultSet,
                                                "batch_id"
                                        ),
                                        resultSet.getString(
                                                "batch_code"
                                        ),
                                        resultSet.getString(
                                                "batch_name"
                                        ),
                                        BatchStatus.valueOf(
                                                resultSet.getString(
                                                        "batch_status"
                                                )
                                        ),
                                        resultSet.getInt(
                                                "batch_capacity"
                                        ),
                                        resultSet.getObject(
                                                "batch_start_date",
                                                LocalDate.class
                                        ),
                                        resultSet.getObject(
                                                "batch_end_date",
                                                LocalDate.class
                                        ),
                                        readUuid(
                                                resultSet,
                                                "branch_id"
                                        ),
                                        resultSet.getString(
                                                "branch_code"
                                        ),
                                        resultSet.getString(
                                                "branch_name"
                                        ),
                                        readUuid(
                                                resultSet,
                                                "course_id"
                                        ),
                                        resultSet.getString(
                                                "course_code"
                                        ),
                                        resultSet.getString(
                                                "course_name"
                                        ),
                                        readUuid(
                                                resultSet,
                                                "course_level_id"
                                        ),
                                        resultSet.getString(
                                                "course_level_code"
                                        ),
                                        resultSet.getString(
                                                "course_level_name"
                                        ),
                                        readUuid(
                                                resultSet,
                                                "instructor_id"
                                        ),
                                        resultSet.getString(
                                                "instructor_name"
                                        )
                                )
                );

        if (batches.isEmpty()) {
            return Optional.empty();
        }

        BatchDetails batch = batches.getFirst();

        SessionStatusSummaryResponse sessionSummary =
                findSessionSummary(
                        null,
                        batchId,
                        fromDate,
                        toDate
                );

        long reservedSeats =
                findCurrentReservedSeats(
                        null,
                        batchId
                );

        AttendanceSummaryResponse attendanceSummary =
                findAttendanceSummary(
                        null,
                        batchId,
                        null,
                        fromDate,
                        toDate
                );

        return Optional.of(
                new BatchAcademicReportResponse(
                        batch.batchId(),
                        batch.batchCode(),
                        batch.batchName(),
                        batch.batchStatus(),

                        batch.branchId(),
                        batch.branchCode(),
                        batch.branchName(),

                        batch.courseId(),
                        batch.courseCode(),
                        batch.courseName(),

                        batch.courseLevelId(),
                        batch.courseLevelCode(),
                        batch.courseLevelName(),

                        batch.instructorId(),
                        batch.instructorName(),

                        batch.capacity(),
                        reservedSeats,

                        batch.batchStartDate(),
                        batch.batchEndDate(),

                        fromDate,
                        toDate,

                        sessionSummary,
                        attendanceSummary
                )
        );
    }

    public StudentAttendanceReportResponse
    findStudentAttendanceReport(
            UUID studentId,
            UUID batchId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        MapSqlParameterSource parameters =
                new MapSqlParameterSource()
                        .addValue("studentId", studentId)
                        .addValue("batchId", batchId)
                        .addValue("fromDate", fromDate)
                        .addValue("toDate", toDate);

        StringBuilder sql = new StringBuilder(
                """
                SELECT
                    COUNT(DISTINCT batch.id)
                        AS attended_batches,

                    COUNT(DISTINCT session.id)
                        AS sessions_with_attendance,

                    COUNT(*)
                        AS total_records,

                    COALESCE(
                        SUM(
                            CASE
                                WHEN attendance.status =
                                     'PRESENT'
                                THEN 1
                                ELSE 0
                            END
                        ),
                        0
                    ) AS present_count,

                    COALESCE(
                        SUM(
                            CASE
                                WHEN attendance.status =
                                     'ABSENT'
                                THEN 1
                                ELSE 0
                            END
                        ),
                        0
                    ) AS absent_count,

                    COALESCE(
                        SUM(
                            CASE
                                WHEN attendance.status =
                                     'LATE'
                                THEN 1
                                ELSE 0
                            END
                        ),
                        0
                    ) AS late_count,

                    COALESCE(
                        SUM(
                            CASE
                                WHEN attendance.status =
                                     'EXCUSED'
                                THEN 1
                                ELSE 0
                            END
                        ),
                        0
                    ) AS excused_count

                FROM attendance_records attendance
                JOIN batch_sessions session
                  ON session.id = attendance.session_id
                JOIN batches batch
                  ON batch.id = session.batch_id
                WHERE attendance.student_id = :studentId
                """
        );

        if (batchId != null) {
            sql.append(
                    """
                     AND batch.id = :batchId
                    """
            );
        }

        appendDateFilters(
                sql,
                "session.session_date",
                fromDate,
                toDate
        );

        StudentAttendanceAggregate aggregate =
                jdbcTemplate.queryForObject(
                        sql.toString(),
                        parameters,
                        (resultSet, rowNumber) ->
                                new StudentAttendanceAggregate(
                                        resultSet.getLong(
                                                "attended_batches"
                                        ),
                                        resultSet.getLong(
                                                "sessions_with_attendance"
                                        ),
                                        mapAttendanceSummary(
                                                resultSet
                                        )
                                )
                );

        if (aggregate == null) {
            aggregate =
                    new StudentAttendanceAggregate(
                            0,
                            0,
                            emptyAttendanceSummary()
                    );
        }

        return new StudentAttendanceReportResponse(
                studentId,
                batchId,
                fromDate,
                toDate,
                aggregate.attendedBatches(),
                aggregate.sessionsWithAttendance(),
                aggregate.attendance()
        );
    }

    private BatchStatusSummaryResponse findBatchSummary(
            UUID branchId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        MapSqlParameterSource parameters =
                reportParameters(
                        branchId,
                        null,
                        null,
                        fromDate,
                        toDate
                );

        StringBuilder sql = new StringBuilder(
                """
                SELECT
                    COUNT(*) AS total_batches,

                    COALESCE(
                        SUM(
                            CASE
                                WHEN batch.status = 'DRAFT'
                                THEN 1
                                ELSE 0
                            END
                        ),
                        0
                    ) AS draft_batches,

                    COALESCE(
                        SUM(
                            CASE
                                WHEN batch.status =
                                     'OPEN_FOR_ENROLLMENT'
                                THEN 1
                                ELSE 0
                            END
                        ),
                        0
                    ) AS open_batches,

                    COALESCE(
                        SUM(
                            CASE
                                WHEN batch.status =
                                     'IN_PROGRESS'
                                THEN 1
                                ELSE 0
                            END
                        ),
                        0
                    ) AS in_progress_batches,

                    COALESCE(
                        SUM(
                            CASE
                                WHEN batch.status =
                                     'COMPLETED'
                                THEN 1
                                ELSE 0
                            END
                        ),
                        0
                    ) AS completed_batches,

                    COALESCE(
                        SUM(
                            CASE
                                WHEN batch.status =
                                     'CANCELLED'
                                THEN 1
                                ELSE 0
                            END
                        ),
                        0
                    ) AS cancelled_batches

                FROM batches batch
                WHERE 1 = 1
                """
        );

        appendBranchFilter(
                sql,
                "batch.branch_id",
                branchId
        );

        if (fromDate != null) {
            sql.append(
                    """
                     AND batch.end_date >= :fromDate
                    """
            );
        }

        if (toDate != null) {
            sql.append(
                    """
                     AND batch.start_date <= :toDate
                    """
            );
        }

        BatchStatusSummaryResponse result =
                jdbcTemplate.queryForObject(
                        sql.toString(),
                        parameters,
                        (resultSet, rowNumber) ->
                                new BatchStatusSummaryResponse(
                                        resultSet.getLong(
                                                "total_batches"
                                        ),
                                        resultSet.getLong(
                                                "draft_batches"
                                        ),
                                        resultSet.getLong(
                                                "open_batches"
                                        ),
                                        resultSet.getLong(
                                                "in_progress_batches"
                                        ),
                                        resultSet.getLong(
                                                "completed_batches"
                                        ),
                                        resultSet.getLong(
                                                "cancelled_batches"
                                        )
                                )
                );

        if (result == null) {
            return new BatchStatusSummaryResponse(
                    0,
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }

        return result;
    }

    private SessionStatusSummaryResponse
    findSessionSummary(
            UUID branchId,
            UUID batchId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        MapSqlParameterSource parameters =
                reportParameters(
                        branchId,
                        batchId,
                        null,
                        fromDate,
                        toDate
                );

        StringBuilder sql = new StringBuilder(
                """
                SELECT
                    COUNT(*) AS total_sessions,

                    COALESCE(
                        SUM(
                            CASE
                                WHEN session.status = 'PLANNED'
                                THEN 1
                                ELSE 0
                            END
                        ),
                        0
                    ) AS planned_sessions,

                    COALESCE(
                        SUM(
                            CASE
                                WHEN session.status = 'COMPLETED'
                                THEN 1
                                ELSE 0
                            END
                        ),
                        0
                    ) AS completed_sessions,

                    COALESCE(
                        SUM(
                            CASE
                                WHEN session.status = 'CANCELLED'
                                THEN 1
                                ELSE 0
                            END
                        ),
                        0
                    ) AS cancelled_sessions

                FROM batch_sessions session
                JOIN batches batch
                  ON batch.id = session.batch_id
                WHERE 1 = 1
                """
        );

        appendBranchFilter(
                sql,
                "batch.branch_id",
                branchId
        );

        appendBatchFilter(
                sql,
                "batch.id",
                batchId
        );

        appendDateFilters(
                sql,
                "session.session_date",
                fromDate,
                toDate
        );

        SessionStatusSummaryResponse result =
                jdbcTemplate.queryForObject(
                        sql.toString(),
                        parameters,
                        (resultSet, rowNumber) ->
                                new SessionStatusSummaryResponse(
                                        resultSet.getLong(
                                                "total_sessions"
                                        ),
                                        resultSet.getLong(
                                                "planned_sessions"
                                        ),
                                        resultSet.getLong(
                                                "completed_sessions"
                                        ),
                                        resultSet.getLong(
                                                "cancelled_sessions"
                                        )
                                )
                );

        if (result == null) {
            return new SessionStatusSummaryResponse(
                    0,
                    0,
                    0,
                    0
            );
        }

        return result;
    }

    private long findCurrentReservedSeats(
            UUID branchId,
            UUID batchId
    ) {
        MapSqlParameterSource parameters =
                reportParameters(
                        branchId,
                        batchId,
                        null,
                        null,
                        null
                );

        StringBuilder sql = new StringBuilder(
                """
                SELECT COUNT(*)
                FROM seat_reservations reservation
                JOIN batches batch
                  ON batch.id = reservation.batch_id
                WHERE reservation.status = 'RESERVED'
                """
        );

        appendBranchFilter(
                sql,
                "batch.branch_id",
                branchId
        );

        appendBatchFilter(
                sql,
                "batch.id",
                batchId
        );

        Long result = jdbcTemplate.queryForObject(
                sql.toString(),
                parameters,
                Long.class
        );

        return result == null ? 0 : result;
    }

    private AttendanceSummaryResponse
    findAttendanceSummary(
            UUID branchId,
            UUID batchId,
            UUID studentId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        MapSqlParameterSource parameters =
                reportParameters(
                        branchId,
                        batchId,
                        studentId,
                        fromDate,
                        toDate
                );

        StringBuilder sql = new StringBuilder(
                """
                SELECT
                    COUNT(*) AS total_records,

                    COALESCE(
                        SUM(
                            CASE
                                WHEN attendance.status =
                                     'PRESENT'
                                THEN 1
                                ELSE 0
                            END
                        ),
                        0
                    ) AS present_count,

                    COALESCE(
                        SUM(
                            CASE
                                WHEN attendance.status =
                                     'ABSENT'
                                THEN 1
                                ELSE 0
                            END
                        ),
                        0
                    ) AS absent_count,

                    COALESCE(
                        SUM(
                            CASE
                                WHEN attendance.status =
                                     'LATE'
                                THEN 1
                                ELSE 0
                            END
                        ),
                        0
                    ) AS late_count,

                    COALESCE(
                        SUM(
                            CASE
                                WHEN attendance.status =
                                     'EXCUSED'
                                THEN 1
                                ELSE 0
                            END
                        ),
                        0
                    ) AS excused_count

                FROM attendance_records attendance
                JOIN batch_sessions session
                  ON session.id = attendance.session_id
                JOIN batches batch
                  ON batch.id = session.batch_id
                WHERE 1 = 1
                """
        );

        appendBranchFilter(
                sql,
                "batch.branch_id",
                branchId
        );

        appendBatchFilter(
                sql,
                "batch.id",
                batchId
        );

        if (studentId != null) {
            sql.append(
                    """
                     AND attendance.student_id = :studentId
                    """
            );
        }

        appendDateFilters(
                sql,
                "session.session_date",
                fromDate,
                toDate
        );

        AttendanceSummaryResponse result =
                jdbcTemplate.queryForObject(
                        sql.toString(),
                        parameters,
                        (resultSet, rowNumber) ->
                                mapAttendanceSummary(
                                        resultSet
                                )
                );

        if (result == null) {
            return emptyAttendanceSummary();
        }

        return result;
    }

    private AttendanceSummaryResponse
    mapAttendanceSummary(
            ResultSet resultSet
    ) throws SQLException {
        long totalRecords =
                resultSet.getLong("total_records");

        long present =
                resultSet.getLong("present_count");

        long absent =
                resultSet.getLong("absent_count");

        long late =
                resultSet.getLong("late_count");

        long excused =
                resultSet.getLong("excused_count");

        long eligibleRecords =
                present + absent + late;

        long attendedRecords =
                present + late;

        BigDecimal attendanceRate =
                calculateAttendanceRate(
                        attendedRecords,
                        eligibleRecords
                );

        return new AttendanceSummaryResponse(
                totalRecords,
                present,
                absent,
                late,
                excused,
                eligibleRecords,
                attendanceRate
        );
    }

    private AttendanceSummaryResponse
    emptyAttendanceSummary() {
        return new AttendanceSummaryResponse(
                0,
                0,
                0,
                0,
                0,
                0,
                BigDecimal.ZERO.setScale(
                        ATTENDANCE_RATE_SCALE
                )
        );
    }

    private BigDecimal calculateAttendanceRate(
            long attendedRecords,
            long eligibleRecords
    ) {
        if (eligibleRecords == 0) {
            return BigDecimal.ZERO.setScale(
                    ATTENDANCE_RATE_SCALE
            );
        }

        return BigDecimal
                .valueOf(attendedRecords)
                .multiply(
                        BigDecimal.valueOf(100)
                )
                .divide(
                        BigDecimal.valueOf(
                                eligibleRecords
                        ),
                        ATTENDANCE_RATE_SCALE,
                        RoundingMode.HALF_UP
                );
    }

    private MapSqlParameterSource reportParameters(
            UUID branchId,
            UUID batchId,
            UUID studentId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return new MapSqlParameterSource()
                .addValue("branchId", branchId)
                .addValue("batchId", batchId)
                .addValue("studentId", studentId)
                .addValue("fromDate", fromDate)
                .addValue("toDate", toDate);
    }

    private void appendBranchFilter(
            StringBuilder sql,
            String column,
            UUID branchId
    ) {
        if (branchId != null) {
            sql.append(
                    " AND "
                            + column
                            + " = :branchId"
            );
        }
    }

    private void appendBatchFilter(
            StringBuilder sql,
            String column,
            UUID batchId
    ) {
        if (batchId != null) {
            sql.append(
                    " AND "
                            + column
                            + " = :batchId"
            );
        }
    }

    private void appendDateFilters(
            StringBuilder sql,
            String column,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        if (fromDate != null) {
            sql.append(
                    " AND "
                            + column
                            + " >= :fromDate"
            );
        }

        if (toDate != null) {
            sql.append(
                    " AND "
                            + column
                            + " <= :toDate"
            );
        }
    }

    private UUID readUuid(
            ResultSet resultSet,
            String column
    ) throws SQLException {
        return resultSet.getObject(
                column,
                UUID.class
        );
    }

    private record BatchDetails(

            UUID batchId,
            String batchCode,
            String batchName,
            BatchStatus batchStatus,
            int capacity,
            LocalDate batchStartDate,
            LocalDate batchEndDate,

            UUID branchId,
            String branchCode,
            String branchName,

            UUID courseId,
            String courseCode,
            String courseName,

            UUID courseLevelId,
            String courseLevelCode,
            String courseLevelName,

            UUID instructorId,
            String instructorName

    ) {
    }

    private record StudentAttendanceAggregate(

            long attendedBatches,
            long sessionsWithAttendance,
            AttendanceSummaryResponse attendance

    ) {
    }
}