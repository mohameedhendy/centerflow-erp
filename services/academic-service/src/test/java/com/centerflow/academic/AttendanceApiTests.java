package com.centerflow.academic;

import com.centerflow.academic.attendance.repository.AttendanceRecordRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AttendanceApiTests {

    private static final String BRANCHES_URL =
            "/api/v1/academic/branches";

    private static final String CLASSROOMS_URL =
            "/api/v1/academic/classrooms";

    private static final String COURSES_URL =
            "/api/v1/academic/courses";

    private static final String LEVELS_URL =
            "/api/v1/academic/course-levels";

    private static final String INSTRUCTORS_URL =
            "/api/v1/academic/instructors";

    private static final String BATCHES_URL =
            "/api/v1/academic/batches";

    private static final String SESSION_URL =
            "/api/v1/academic/batch-sessions";

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;
    private final AttendanceRecordRepository
            attendanceRecordRepository;

    @Autowired
    AttendanceApiTests(
            MockMvc mockMvc,
            JdbcTemplate jdbcTemplate,
            AttendanceRecordRepository
                    attendanceRecordRepository
    ) {
        this.mockMvc = mockMvc;
        this.jdbcTemplate = jdbcTemplate;
        this.attendanceRecordRepository =
                attendanceRecordRepository;
    }

    @AfterEach
    void cleanDatabase() {
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

    @Test
    void marksAttendanceForMultipleStudents()
            throws Exception {

        SessionResources resources =
                createSessionResources("MARK");

        UUID firstEnrollmentId =
                UUID.randomUUID();

        UUID firstStudentId =
                UUID.randomUUID();

        UUID secondEnrollmentId =
                UUID.randomUUID();

        UUID secondStudentId =
                UUID.randomUUID();

        mockMvc.perform(
                        put(
                                SESSION_URL
                                        + "/"
                                        + resources.sessionId()
                                        + "/attendance"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        attendanceRequest(
                                                firstEnrollmentId,
                                                firstStudentId,
                                                "PRESENT",
                                                "Arrived on time",
                                                secondEnrollmentId,
                                                secondStudentId,
                                                "LATE",
                                                "Arrived 15 minutes late"
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.records.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath(
                                "$.records[0].sessionId"
                        ).value(
                                resources.sessionId()
                                        .toString()
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.records[0].status"
                        ).value("PRESENT")
                )
                .andExpect(
                        jsonPath(
                                "$.records[1].status"
                        ).value("LATE")
                );

        assertThat(
                attendanceRecordRepository.count()
        ).isEqualTo(2);
    }

    @Test
    void updatesExistingAttendanceRecord()
            throws Exception {

        SessionResources resources =
                createSessionResources("UPDATE");

        UUID enrollmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        MvcResult firstResult = markSingleAttendance(
                resources.sessionId(),
                enrollmentId,
                studentId,
                "ABSENT",
                "Student did not attend"
        );

        UUID attendanceId =
                extractFirstAttendanceId(firstResult);

        mockMvc.perform(
                        put(
                                SESSION_URL
                                        + "/"
                                        + resources.sessionId()
                                        + "/attendance"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        singleAttendanceRequest(
                                                enrollmentId,
                                                studentId,
                                                "EXCUSED",
                                                "Medical excuse"
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.records.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.records[0].id")
                                .value(
                                        attendanceId.toString()
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.records[0].status"
                        ).value("EXCUSED")
                )
                .andExpect(
                        jsonPath(
                                "$.records[0].notes"
                        ).value("Medical excuse")
                );

        assertThat(
                attendanceRecordRepository.count()
        ).isEqualTo(1);
    }

    @Test
    void rejectsDuplicateReferencesInsideSameRequest()
            throws Exception {

        SessionResources resources =
                createSessionResources("DUPLICATE");

        UUID enrollmentId = UUID.randomUUID();

        mockMvc.perform(
                        put(
                                SESSION_URL
                                        + "/"
                                        + resources.sessionId()
                                        + "/attendance"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        attendanceRequest(
                                                enrollmentId,
                                                UUID.randomUUID(),
                                                "PRESENT",
                                                null,
                                                enrollmentId,
                                                UUID.randomUUID(),
                                                "ABSENT",
                                                null
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Attendance request contains duplicate enrollment ID: "
                                                + enrollmentId
                                )
                );

        assertThat(
                attendanceRecordRepository.count()
        ).isZero();
    }

    @Test
    void rejectsAttendanceForCancelledSession()
            throws Exception {

        SessionResources resources =
                createSessionResources("CANCELLED");

        changeSessionStatus(
                resources.sessionId(),
                "CANCELLED"
        );

        mockMvc.perform(
                        put(
                                SESSION_URL
                                        + "/"
                                        + resources.sessionId()
                                        + "/attendance"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        singleAttendanceRequest(
                                                UUID.randomUUID(),
                                                UUID.randomUUID(),
                                                "PRESENT",
                                                null
                                        )
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Attendance cannot be recorded for a cancelled session"
                                )
                );

        assertThat(
                attendanceRecordRepository.count()
        ).isZero();
    }

    @Test
    void searchesAttendanceBySessionAndBatch()
            throws Exception {

        SessionResources resources =
                createSessionResources("SEARCH");

        UUID firstEnrollmentId =
                UUID.randomUUID();

        UUID firstStudentId =
                UUID.randomUUID();

        UUID secondEnrollmentId =
                UUID.randomUUID();

        UUID secondStudentId =
                UUID.randomUUID();

        mockMvc.perform(
                        put(
                                SESSION_URL
                                        + "/"
                                        + resources.sessionId()
                                        + "/attendance"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        attendanceRequest(
                                                firstEnrollmentId,
                                                firstStudentId,
                                                "PRESENT",
                                                null,
                                                secondEnrollmentId,
                                                secondStudentId,
                                                "ABSENT",
                                                "No excuse"
                                        )
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get(
                                SESSION_URL
                                        + "/"
                                        + resources.sessionId()
                                        + "/attendance"
                        )
                                .queryParam(
                                        "status",
                                        "PRESENT"
                                )
                                .queryParam("page", "0")
                                .queryParam("size", "20")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].studentId"
                        ).value(
                                firstStudentId.toString()
                        )
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(1)
                );

        mockMvc.perform(
                        get(
                                BATCHES_URL
                                        + "/"
                                        + resources.batchId()
                                        + "/attendance"
                        )
                                .queryParam(
                                        "studentId",
                                        secondStudentId.toString()
                                )
                                .queryParam(
                                        "status",
                                        "ABSENT"
                                )
                                .queryParam(
                                        "dateFrom",
                                        "2026-09-01"
                                )
                                .queryParam(
                                        "dateTo",
                                        "2026-09-30"
                                )
                                .queryParam("page", "0")
                                .queryParam("size", "20")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].enrollmentId"
                        ).value(
                                secondEnrollmentId.toString()
                        )
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(1)
                );
    }

    @Test
    void returnsAttendanceSummary()
            throws Exception {

        SessionResources resources =
                createSessionResources("SUMMARY");

        markSingleAttendance(
                resources.sessionId(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PRESENT",
                null
        );

        markSingleAttendance(
                resources.sessionId(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ABSENT",
                null
        );

        markSingleAttendance(
                resources.sessionId(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "LATE",
                null
        );

        markSingleAttendance(
                resources.sessionId(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "EXCUSED",
                null
        );

        mockMvc.perform(
                        get(
                                SESSION_URL
                                        + "/"
                                        + resources.sessionId()
                                        + "/attendance/summary"
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.sessionId")
                                .value(
                                        resources.sessionId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.total")
                                .value(4)
                )
                .andExpect(
                        jsonPath("$.present")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.absent")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.late")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.excused")
                                .value(1)
                );
    }

    @Test
    void allowsOnlyOneConcurrentRecordForSameStudent()
            throws Exception {

        String suffix = UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();

        SessionResources resources =
                createSessionResources(
                        "CON-" + suffix
                );

        UUID studentId = UUID.randomUUID();

        UUID firstEnrollmentId =
                UUID.randomUUID();

        UUID secondEnrollmentId =
                UUID.randomUUID();

        ExecutorService executorService =
                Executors.newFixedThreadPool(2);

        CountDownLatch readyLatch =
                new CountDownLatch(2);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        try {
            Future<Integer> firstResult =
                    executorService.submit(
                            () -> markConcurrently(
                                    resources.sessionId(),
                                    firstEnrollmentId,
                                    studentId,
                                    readyLatch,
                                    startLatch
                            )
                    );

            Future<Integer> secondResult =
                    executorService.submit(
                            () -> markConcurrently(
                                    resources.sessionId(),
                                    secondEnrollmentId,
                                    studentId,
                                    readyLatch,
                                    startLatch
                            )
                    );

            assertThat(
                    readyLatch.await(
                            5,
                            TimeUnit.SECONDS
                    )
            ).isTrue();

            startLatch.countDown();

            List<Integer> statuses = List.of(
                    firstResult.get(
                            15,
                            TimeUnit.SECONDS
                    ),
                    secondResult.get(
                            15,
                            TimeUnit.SECONDS
                    )
            );

            assertThat(statuses)
                    .containsExactlyInAnyOrder(
                            200,
                            409
                    );

        } finally {
            executorService.shutdownNow();

            executorService.awaitTermination(
                    5,
                    TimeUnit.SECONDS
            );
        }

        assertThat(
                attendanceRecordRepository.count()
        ).isEqualTo(1);
    }

    private int markConcurrently(
            UUID sessionId,
            UUID enrollmentId,
            UUID studentId,
            CountDownLatch readyLatch,
            CountDownLatch startLatch
    ) throws Exception {

        readyLatch.countDown();

        boolean started = startLatch.await(
                5,
                TimeUnit.SECONDS
        );

        if (!started) {
            throw new IllegalStateException(
                    "Concurrent attendance test did not start in time"
            );
        }

        return mockMvc.perform(
                        put(
                                SESSION_URL
                                        + "/"
                                        + sessionId
                                        + "/attendance"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        singleAttendanceRequest(
                                                enrollmentId,
                                                studentId,
                                                "PRESENT",
                                                null
                                        )
                                )
                )
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private SessionResources createSessionResources(
            String suffix
    ) throws Exception {

        UUID branchId = createBranch(
                "BR-" + suffix,
                suffix + " Branch"
        );

        UUID classroomId = createClassroom(
                branchId,
                "ROOM-" + suffix,
                50
        );

        UUID courseId = createCourse(
                "COURSE-" + suffix,
                suffix + " Course"
        );

        UUID courseLevelId = createCourseLevel(
                courseId,
                "LEVEL-" + suffix
        );

        UUID instructorId = createInstructor(
                "INS-" + suffix,
                suffix.toLowerCase()
                        + "@centerflow.com"
        );

        UUID batchId = createBatch(
                "ATT-" + suffix,
                branchId,
                classroomId,
                courseLevelId,
                instructorId
        );

        UUID sessionId = createSession(
                batchId
        );

        return new SessionResources(
                batchId,
                sessionId
        );
    }

    private UUID createBranch(
            String code,
            String name
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post(BRANCHES_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "code": "%s",
                                          "name": "%s",
                                          "city": "Cairo"
                                        }
                                        """.formatted(
                                                code,
                                                name
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    private UUID createClassroom(
            UUID branchId,
            String code,
            int capacity
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post(CLASSROOMS_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "branchId": "%s",
                                          "code": "%s",
                                          "name": "Attendance Classroom",
                                          "capacity": %d,
                                          "floor": "First Floor"
                                        }
                                        """.formatted(
                                                branchId,
                                                code,
                                                capacity
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    private UUID createCourse(
            String code,
            String name
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post(COURSES_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "code": "%s",
                                          "name": "%s",
                                          "description": "Attendance test course"
                                        }
                                        """.formatted(
                                                code,
                                                name
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    private UUID createCourseLevel(
            UUID courseId,
            String code
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post(LEVELS_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "courseId": "%s",
                                          "code": "%s",
                                          "name": "Attendance Test Level",
                                          "sequenceNumber": 1,
                                          "durationHours": 60,
                                          "description": "Attendance test level"
                                        }
                                        """.formatted(
                                                courseId,
                                                code
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    private UUID createInstructor(
            String code,
            String email
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post(INSTRUCTORS_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "code": "%s",
                                          "firstName": "Ahmed",
                                          "lastName": "Mohamed",
                                          "email": "%s",
                                          "phone": "+20 100 000 0000",
                                          "specialization": "Java Backend",
                                          "bio": "Attendance test instructor"
                                        }
                                        """.formatted(
                                                code,
                                                email
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    private UUID createBatch(
            String code,
            UUID branchId,
            UUID classroomId,
            UUID courseLevelId,
            UUID instructorId
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post(BATCHES_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "code": "%s",
                                          "name": "%s Batch",
                                          "branchId": "%s",
                                          "classroomId": "%s",
                                          "courseLevelId": "%s",
                                          "instructorId": "%s",
                                          "capacity": 30,
                                          "startDate": "2026-09-01",
                                          "endDate": "2026-12-31"
                                        }
                                        """.formatted(
                                                code,
                                                code,
                                                branchId,
                                                classroomId,
                                                courseLevelId,
                                                instructorId
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    private UUID createSession(
            UUID batchId
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post(
                                BATCHES_URL
                                        + "/"
                                        + batchId
                                        + "/sessions"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "sessionDate": "2026-09-10",
                                          "startTime": "10:00:00",
                                          "endTime": "12:00:00",
                                          "topic": "Attendance Test Session"
                                        }
                                        """
                                )
                )
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    private MvcResult markSingleAttendance(
            UUID sessionId,
            UUID enrollmentId,
            UUID studentId,
            String statusValue,
            String notes
    ) throws Exception {

        return mockMvc.perform(
                        put(
                                SESSION_URL
                                        + "/"
                                        + sessionId
                                        + "/attendance"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        singleAttendanceRequest(
                                                enrollmentId,
                                                studentId,
                                                statusValue,
                                                notes
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andReturn();
    }

    private void changeSessionStatus(
            UUID sessionId,
            String statusValue
    ) throws Exception {

        mockMvc.perform(
                        patch(
                                SESSION_URL
                                        + "/"
                                        + sessionId
                                        + "/status"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "status": "%s"
                                        }
                                        """.formatted(statusValue)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value(statusValue)
                );
    }

    private String singleAttendanceRequest(
            UUID enrollmentId,
            UUID studentId,
            String statusValue,
            String notes
    ) {
        return """
                {
                  "records": [
                    {
                      "enrollmentId": "%s",
                      "studentId": "%s",
                      "status": "%s",
                      "notes": %s
                    }
                  ]
                }
                """.formatted(
                enrollmentId,
                studentId,
                statusValue,
                jsonStringOrNull(notes)
        );
    }

    private String attendanceRequest(
            UUID firstEnrollmentId,
            UUID firstStudentId,
            String firstStatus,
            String firstNotes,
            UUID secondEnrollmentId,
            UUID secondStudentId,
            String secondStatus,
            String secondNotes
    ) {
        return """
                {
                  "records": [
                    {
                      "enrollmentId": "%s",
                      "studentId": "%s",
                      "status": "%s",
                      "notes": %s
                    },
                    {
                      "enrollmentId": "%s",
                      "studentId": "%s",
                      "status": "%s",
                      "notes": %s
                    }
                  ]
                }
                """.formatted(
                firstEnrollmentId,
                firstStudentId,
                firstStatus,
                jsonStringOrNull(firstNotes),
                secondEnrollmentId,
                secondStudentId,
                secondStatus,
                jsonStringOrNull(secondNotes)
        );
    }

    private String jsonStringOrNull(
            String value
    ) {
        if (value == null) {
            return "null";
        }

        return "\""
                + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                + "\"";
    }

    private UUID extractId(
            MvcResult result
    ) throws Exception {

        String id = JsonPath.read(
                result.getResponse()
                        .getContentAsString(),
                "$.id"
        );

        return UUID.fromString(id);
    }

    private UUID extractFirstAttendanceId(
            MvcResult result
    ) throws Exception {

        String id = JsonPath.read(
                result.getResponse()
                        .getContentAsString(),
                "$.records[0].id"
        );

        return UUID.fromString(id);
    }

    private record SessionResources(
            UUID batchId,
            UUID sessionId
    ) {
    }
}