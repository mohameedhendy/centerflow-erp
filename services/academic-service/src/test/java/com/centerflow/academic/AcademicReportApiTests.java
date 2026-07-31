package com.centerflow.academic;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AcademicReportApiTests {

    private static final String REPORTS_URL =
            "/api/v1/academic/reports";

    private static final LocalDate REPORT_DATE =
            LocalDate.of(2026, 7, 30);

    private final UUID firstBranchId =
            UUID.randomUUID();

    private final UUID secondBranchId =
            UUID.randomUUID();

    private final UUID firstClassroomId =
            UUID.randomUUID();

    private final UUID secondClassroomId =
            UUID.randomUUID();

    private final UUID courseId =
            UUID.randomUUID();

    private final UUID courseLevelId =
            UUID.randomUUID();

    private final UUID instructorId =
            UUID.randomUUID();

    private final UUID firstBatchId =
            UUID.randomUUID();

    private final UUID secondBatchId =
            UUID.randomUUID();

    private final UUID thirdBatchId =
            UUID.randomUUID();

    private final UUID firstSessionId =
            UUID.randomUUID();

    private final UUID secondSessionId =
            UUID.randomUUID();

    private final UUID thirdSessionId =
            UUID.randomUUID();

    private final UUID fourthSessionId =
            UUID.randomUUID();

    private final UUID fifthSessionId =
            UUID.randomUUID();

    private final UUID firstStudentId =
            UUID.randomUUID();

    private final UUID secondStudentId =
            UUID.randomUUID();

    private final UUID thirdStudentId =
            UUID.randomUUID();

    private final UUID fourthStudentId =
            UUID.randomUUID();

    private final UUID fifthStudentId =
            UUID.randomUUID();

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    AcademicReportApiTests(
            MockMvc mockMvc,
            JdbcTemplate jdbcTemplate
    ) {
        this.mockMvc = mockMvc;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        cleanDatabase();
        insertFixtures();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void overviewAggregatesAcademicDataInDatabase()
            throws Exception {
        mockMvc.perform(
                        get(
                                REPORTS_URL
                                        + "/overview"
                        )
                                .param(
                                        "branchId",
                                        firstBranchId
                                                .toString()
                                )
                                .param(
                                        "fromDate",
                                        REPORT_DATE
                                                .minusDays(1)
                                                .toString()
                                )
                                .param(
                                        "toDate",
                                        REPORT_DATE
                                                .toString()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.branchId")
                                .value(
                                        firstBranchId
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.batches.totalBatches"
                        ).value(2)
                )
                .andExpect(
                        jsonPath(
                                "$.batches.inProgressBatches"
                        ).value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.batches.completedBatches"
                        ).value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.sessions.totalSessions"
                        ).value(4)
                )
                .andExpect(
                        jsonPath(
                                "$.sessions.plannedSessions"
                        ).value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.sessions.completedSessions"
                        ).value(3)
                )
                .andExpect(
                        jsonPath(
                                "$.currentlyReservedSeats"
                        ).value(5)
                )
                .andExpect(
                        jsonPath(
                                "$.attendance.totalRecords"
                        ).value(7)
                )
                .andExpect(
                        jsonPath(
                                "$.attendance.present"
                        ).value(2)
                )
                .andExpect(
                        jsonPath(
                                "$.attendance.absent"
                        ).value(2)
                )
                .andExpect(
                        jsonPath(
                                "$.attendance.late"
                        ).value(2)
                )
                .andExpect(
                        jsonPath(
                                "$.attendance.excused"
                        ).value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.attendance.rateEligibleRecords"
                        ).value(6)
                )
                .andExpect(
                        jsonPath(
                                "$.attendance.attendanceRate"
                        ).value(66.67)
                );
    }

    @Test
    void batchReportReturnsBatchAndAttendanceSummary()
            throws Exception {
        mockMvc.perform(
                        get(
                                REPORTS_URL
                                        + "/batches/"
                                        + firstBatchId
                        )
                                .param(
                                        "fromDate",
                                        REPORT_DATE
                                                .minusDays(1)
                                                .toString()
                                )
                                .param(
                                        "toDate",
                                        REPORT_DATE
                                                .toString()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.batchId")
                                .value(
                                        firstBatchId
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.batchCode")
                                .value("BATCH-ONE")
                )
                .andExpect(
                        jsonPath("$.batchStatus")
                                .value("IN_PROGRESS")
                )
                .andExpect(
                        jsonPath("$.branchName")
                                .value("Main Branch")
                )
                .andExpect(
                        jsonPath("$.courseName")
                                .value("Java Backend")
                )
                .andExpect(
                        jsonPath("$.courseLevelName")
                                .value("Spring Boot")
                )
                .andExpect(
                        jsonPath("$.instructorName")
                                .value("Mohamed Instructor")
                )
                .andExpect(
                        jsonPath("$.capacity")
                                .value(20)
                )
                .andExpect(
                        jsonPath(
                                "$.currentlyReservedSeats"
                        ).value(3)
                )
                .andExpect(
                        jsonPath(
                                "$.sessions.totalSessions"
                        ).value(3)
                )
                .andExpect(
                        jsonPath(
                                "$.sessions.completedSessions"
                        ).value(2)
                )
                .andExpect(
                        jsonPath(
                                "$.sessions.plannedSessions"
                        ).value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.attendance.totalRecords"
                        ).value(6)
                )
                .andExpect(
                        jsonPath(
                                "$.attendance.attendanceRate"
                        ).value(80.00)
                );
    }

    @Test
    void studentReportSupportsBatchAndDateFilters()
            throws Exception {
        mockMvc.perform(
                        get(
                                REPORTS_URL
                                        + "/students/"
                                        + firstStudentId
                                        + "/attendance"
                        )
                                .param(
                                        "batchId",
                                        firstBatchId
                                                .toString()
                                )
                                .param(
                                        "fromDate",
                                        REPORT_DATE
                                                .toString()
                                )
                                .param(
                                        "toDate",
                                        REPORT_DATE
                                                .toString()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.studentId")
                                .value(
                                        firstStudentId
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.batchId")
                                .value(
                                        firstBatchId
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.attendedBatches")
                                .value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.sessionsWithAttendance"
                        ).value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.attendance.totalRecords"
                        ).value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.attendance.late"
                        ).value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.attendance.attendanceRate"
                        ).value(100.00)
                );
    }

    @Test
    void invalidReportPeriodReturnsBadRequest()
            throws Exception {
        mockMvc.perform(
                        get(
                                REPORTS_URL
                                        + "/overview"
                        )
                                .param(
                                        "fromDate",
                                        REPORT_DATE
                                                .plusDays(1)
                                                .toString()
                                )
                                .param(
                                        "toDate",
                                        REPORT_DATE
                                                .toString()
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Academic report from date "
                                                + "must not be after to date"
                                )
                );
    }

    @Test
    void missingBatchReportReturnsNotFound()
            throws Exception {
        UUID missingBatchId =
                UUID.randomUUID();

        mockMvc.perform(
                        get(
                                REPORTS_URL
                                        + "/batches/"
                                        + missingBatchId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Batch was not found: "
                                                + missingBatchId
                                )
                );
    }

    private void insertFixtures() {
        Instant now = Instant.now();

        insertBranch(
                firstBranchId,
                "BRANCH-ONE",
                "Main Branch",
                now
        );

        insertBranch(
                secondBranchId,
                "BRANCH-TWO",
                "Other Branch",
                now
        );

        insertClassroom(
                firstClassroomId,
                firstBranchId,
                "ROOM-ONE",
                "Main Classroom",
                now
        );

        insertClassroom(
                secondClassroomId,
                secondBranchId,
                "ROOM-TWO",
                "Other Classroom",
                now
        );

        jdbcTemplate.update(
                """
                INSERT INTO courses
                (
                    id,
                    code,
                    name,
                    description,
                    active,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                courseId,
                "JAVA-BACKEND",
                "Java Backend",
                "Backend development course",
                true,
                now,
                now
        );

        jdbcTemplate.update(
                """
                INSERT INTO course_levels
                (
                    id,
                    course_id,
                    code,
                    name,
                    sequence_number,
                    duration_hours,
                    description,
                    active,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                courseLevelId,
                courseId,
                "SPRING-BOOT",
                "Spring Boot",
                1,
                60,
                "Spring Boot course level",
                true,
                now,
                now
        );

        jdbcTemplate.update(
                """
                INSERT INTO instructors
                (
                    id,
                    code,
                    first_name,
                    last_name,
                    email,
                    specialization,
                    active,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                instructorId,
                "INST-ONE",
                "Mohamed",
                "Instructor",
                "instructor@centerflow.test",
                "Java Backend",
                true,
                now,
                now
        );

        insertBatch(
                firstBatchId,
                "BATCH-ONE",
                "First Batch",
                firstBranchId,
                firstClassroomId,
                "IN_PROGRESS",
                20,
                now
        );

        insertBatch(
                secondBatchId,
                "BATCH-TWO",
                "Second Batch",
                firstBranchId,
                firstClassroomId,
                "COMPLETED",
                15,
                now
        );

        insertBatch(
                thirdBatchId,
                "BATCH-THREE",
                "Other Branch Batch",
                secondBranchId,
                secondClassroomId,
                "DRAFT",
                10,
                now
        );

        insertSession(
                firstSessionId,
                firstBatchId,
                REPORT_DATE.minusDays(1),
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                "COMPLETED",
                now
        );

        insertSession(
                secondSessionId,
                firstBatchId,
                REPORT_DATE,
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                "COMPLETED",
                now
        );

        insertSession(
                thirdSessionId,
                firstBatchId,
                REPORT_DATE,
                LocalTime.of(20, 0),
                LocalTime.of(22, 0),
                "PLANNED",
                now
        );

        insertSession(
                fourthSessionId,
                secondBatchId,
                REPORT_DATE,
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                "COMPLETED",
                now
        );

        insertSession(
                fifthSessionId,
                thirdBatchId,
                REPORT_DATE,
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                "PLANNED",
                now
        );

        insertReservedSeat(
                firstBatchId,
                "RESERVED",
                now
        );

        insertReservedSeat(
                firstBatchId,
                "RESERVED",
                now
        );

        insertReservedSeat(
                firstBatchId,
                "RESERVED",
                now
        );

        insertReservedSeat(
                firstBatchId,
                "RELEASED",
                now
        );

        insertReservedSeat(
                secondBatchId,
                "RESERVED",
                now
        );

        insertReservedSeat(
                secondBatchId,
                "RESERVED",
                now
        );

        insertReservedSeat(
                thirdBatchId,
                "RESERVED",
                now
        );

        insertAttendance(
                firstSessionId,
                firstStudentId,
                "PRESENT",
                now
        );

        insertAttendance(
                firstSessionId,
                secondStudentId,
                "ABSENT",
                now
        );

        insertAttendance(
                firstSessionId,
                thirdStudentId,
                "LATE",
                now
        );

        insertAttendance(
                firstSessionId,
                fourthStudentId,
                "EXCUSED",
                now
        );

        insertAttendance(
                secondSessionId,
                firstStudentId,
                "LATE",
                now
        );

        insertAttendance(
                secondSessionId,
                secondStudentId,
                "PRESENT",
                now
        );

        insertAttendance(
                fourthSessionId,
                fifthStudentId,
                "ABSENT",
                now
        );
    }

    private void insertBranch(
            UUID id,
            String code,
            String name,
            Instant now
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO branches
                (
                    id,
                    code,
                    name,
                    active,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                id,
                code,
                name,
                true,
                now,
                now
        );
    }

    private void insertClassroom(
            UUID id,
            UUID branchId,
            String code,
            String name,
            Instant now
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO classrooms
                (
                    id,
                    branch_id,
                    code,
                    name,
                    capacity,
                    active,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                branchId,
                code,
                name,
                30,
                true,
                now,
                now
        );
    }

    private void insertBatch(
            UUID id,
            String code,
            String name,
            UUID branchId,
            UUID classroomId,
            String status,
            int capacity,
            Instant now
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO batches
                (
                    id,
                    code,
                    name,
                    branch_id,
                    classroom_id,
                    course_level_id,
                    instructor_id,
                    capacity,
                    start_date,
                    end_date,
                    status,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                )
                """,
                id,
                code,
                name,
                branchId,
                classroomId,
                courseLevelId,
                instructorId,
                capacity,
                REPORT_DATE.minusDays(10),
                REPORT_DATE.plusDays(30),
                status,
                now,
                now
        );
    }

    private void insertSession(
            UUID sessionId,
            UUID batchId,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            String status,
            Instant now
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO batch_sessions
                (
                    id,
                    batch_id,
                    session_date,
                    start_time,
                    end_time,
                    topic,
                    status,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                sessionId,
                batchId,
                sessionDate,
                startTime,
                endTime,
                "Academic report test session",
                status,
                now,
                now
        );
    }

    private void insertReservedSeat(
            UUID batchId,
            String status,
            Instant now
    ) {
        Instant releasedAt =
                "RELEASED".equals(status)
                        ? now
                        : null;

        jdbcTemplate.update(
                """
                INSERT INTO seat_reservations
                (
                    id,
                    batch_id,
                    enrollment_id,
                    status,
                    reserved_at,
                    released_at,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                batchId,
                UUID.randomUUID(),
                status,
                now,
                releasedAt,
                now,
                now
        );
    }

    private void insertAttendance(
            UUID sessionId,
            UUID studentId,
            String status,
            Instant now
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO attendance_records
                (
                    id,
                    session_id,
                    enrollment_id,
                    student_id,
                    status,
                    notes,
                    marked_at,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                sessionId,
                UUID.randomUUID(),
                studentId,
                status,
                "Academic report fixture",
                now,
                now,
                now
        );
    }

    private void cleanDatabase() {
        jdbcTemplate.update(
                "DELETE FROM attendance_records"
        );

        jdbcTemplate.update(
                "DELETE FROM seat_reservations"
        );

        jdbcTemplate.update(
                "DELETE FROM batch_sessions"
        );

        jdbcTemplate.update(
                "DELETE FROM batch_schedules"
        );

        jdbcTemplate.update(
                "DELETE FROM batches"
        );

        jdbcTemplate.update(
                "DELETE FROM instructors"
        );

        jdbcTemplate.update(
                "DELETE FROM course_levels"
        );

        jdbcTemplate.update(
                "DELETE FROM courses"
        );

        jdbcTemplate.update(
                "DELETE FROM classrooms"
        );

        jdbcTemplate.update(
                "DELETE FROM branches"
        );
    }
}