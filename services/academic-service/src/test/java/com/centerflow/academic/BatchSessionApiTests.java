package com.centerflow.academic;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BatchSessionApiTests {

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

    @Autowired
    BatchSessionApiTests(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void createsAndSearchesManualSession()
            throws Exception {

        AcademicResources resources =
                createResources("CREATE");

        UUID batchId = createBatch(
                "SES-CREATE",
                resources,
                "2026-09-01",
                "2026-12-31"
        );

        UUID sessionId = createManualSession(
                batchId,
                "2026-09-10",
                "10:00:00",
                "12:00:00",
                " Core Java "
        );

        mockMvc.perform(
                        get(
                                BATCHES_URL
                                        + "/"
                                        + batchId
                                        + "/sessions"
                        )
                                .queryParam(
                                        "dateFrom",
                                        "2026-09-01"
                                )
                                .queryParam(
                                        "dateTo",
                                        "2026-09-30"
                                )
                                .queryParam(
                                        "status",
                                        "PLANNED"
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
                        jsonPath("$.content[0].id")
                                .value(sessionId.toString())
                )
                .andExpect(
                        jsonPath("$.content[0].batchId")
                                .value(batchId.toString())
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].sessionDate"
                        ).value("2026-09-10")
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].startTime"
                        ).value("10:00:00")
                )
                .andExpect(
                        jsonPath("$.content[0].topic")
                                .value("Core Java")
                )
                .andExpect(
                        jsonPath("$.content[0].status")
                                .value("PLANNED")
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(1)
                );
    }

    @Test
    void generatesSessionsAndSkipsExistingOnSecondRun()
            throws Exception {

        AcademicResources resources =
                createResources("GENERATE");

        UUID batchId = createBatch(
                "SES-GEN",
                resources,
                "2026-09-01",
                "2026-09-30"
        );

        UUID scheduleId = createWeeklySchedule(
                batchId,
                "MONDAY",
                "10:00:00",
                "12:00:00"
        );

        mockMvc.perform(
                        post(
                                BATCHES_URL
                                        + "/"
                                        + batchId
                                        + "/sessions/generate"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "dateFrom": "2026-09-01",
                                          "dateTo": "2026-09-30"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.generatedCount")
                                .value(4)
                )
                .andExpect(
                        jsonPath("$.skippedCount")
                                .value(0)
                );

        mockMvc.perform(
                        post(
                                BATCHES_URL
                                        + "/"
                                        + batchId
                                        + "/sessions/generate"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "dateFrom": "2026-09-01",
                                          "dateTo": "2026-09-30"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.generatedCount")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.skippedCount")
                                .value(4)
                );

        mockMvc.perform(
                        get(
                                BATCHES_URL
                                        + "/"
                                        + batchId
                                        + "/sessions"
                        )
                                .queryParam("page", "0")
                                .queryParam("size", "20")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(4)
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(4)
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].batchScheduleId"
                        ).value(scheduleId.toString())
                );
    }

    @Test
    void rejectsClassroomConflictAcrossBatches()
            throws Exception {

        AcademicResources firstResources =
                createResources("ROOM");

        UUID secondInstructorId =
                createInstructor(
                        "INS-ROOM-2",
                        "room-2@centerflow.com"
                );

        AcademicResources secondResources =
                new AcademicResources(
                        firstResources.branchId(),
                        firstResources.classroomId(),
                        firstResources.courseLevelId(),
                        secondInstructorId
                );

        UUID firstBatchId = createBatch(
                "SES-ROOM-A",
                firstResources,
                "2026-09-01",
                "2026-12-31"
        );

        UUID secondBatchId = createBatch(
                "SES-ROOM-B",
                secondResources,
                "2026-09-01",
                "2026-12-31"
        );

        createManualSession(
                firstBatchId,
                "2026-09-10",
                "10:00:00",
                "12:00:00",
                "First session"
        );

        mockMvc.perform(
                        post(
                                BATCHES_URL
                                        + "/"
                                        + secondBatchId
                                        + "/sessions"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        sessionRequest(
                                                "2026-09-10",
                                                "11:00:00",
                                                "13:00:00",
                                                "Conflicting session"
                                        )
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Classroom "
                                                + firstResources
                                                .classroomId()
                                                + " already has a session on 2026-09-10 during the requested time"
                                )
                );
    }

    @Test
    void rejectsInstructorConflictAcrossDifferentClassrooms()
            throws Exception {

        AcademicResources firstResources =
                createResources("INSTRUCTOR");

        UUID secondClassroomId =
                createClassroom(
                        firstResources.branchId(),
                        "ROOM-INS-2",
                        50
                );

        AcademicResources secondResources =
                new AcademicResources(
                        firstResources.branchId(),
                        secondClassroomId,
                        firstResources.courseLevelId(),
                        firstResources.instructorId()
                );

        UUID firstBatchId = createBatch(
                "SES-INS-A",
                firstResources,
                "2026-09-01",
                "2026-12-31"
        );

        UUID secondBatchId = createBatch(
                "SES-INS-B",
                secondResources,
                "2026-09-01",
                "2026-12-31"
        );

        createManualSession(
                firstBatchId,
                "2026-09-11",
                "14:00:00",
                "16:00:00",
                "First instructor session"
        );

        mockMvc.perform(
                        post(
                                BATCHES_URL
                                        + "/"
                                        + secondBatchId
                                        + "/sessions"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        sessionRequest(
                                                "2026-09-11",
                                                "15:00:00",
                                                "17:00:00",
                                                "Conflicting instructor session"
                                        )
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Instructor "
                                                + firstResources
                                                .instructorId()
                                                + " already has a session on 2026-09-11 during the requested time"
                                )
                );
    }

    @Test
    void allowsBackToBackSessions()
            throws Exception {

        AcademicResources resources =
                createResources("BACK");

        UUID batchId = createBatch(
                "SES-BACK",
                resources,
                "2026-09-01",
                "2026-12-31"
        );

        createManualSession(
                batchId,
                "2026-09-12",
                "10:00:00",
                "12:00:00",
                "First session"
        );

        mockMvc.perform(
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
                                        sessionRequest(
                                                "2026-09-12",
                                                "12:00:00",
                                                "14:00:00",
                                                "Second session"
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.startTime")
                                .value("12:00:00")
                )
                .andExpect(
                        jsonPath("$.endTime")
                                .value("14:00:00")
                );
    }

    @Test
    void completesSessionOnlyWhileBatchIsInProgress()
            throws Exception {

        AcademicResources resources =
                createResources("COMPLETE");

        UUID batchId = createBatch(
                "SES-COMPLETE",
                resources,
                "2026-09-01",
                "2026-12-31"
        );

        UUID sessionId = createManualSession(
                batchId,
                "2026-09-13",
                "10:00:00",
                "12:00:00",
                "Spring Data JPA"
        );

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
                                          "status": "COMPLETED"
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "A session can only be completed while the batch is IN_PROGRESS"
                                )
                );

        changeBatchStatus(
                batchId,
                "OPEN_FOR_ENROLLMENT"
        );

        changeBatchStatus(
                batchId,
                "IN_PROGRESS"
        );

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
                                          "status": "COMPLETED"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("COMPLETED")
                );
    }

    @Test
    void cancelsAndRestoresPlannedSession()
            throws Exception {

        AcademicResources resources =
                createResources("RESTORE");

        UUID batchId = createBatch(
                "SES-RESTORE",
                resources,
                "2026-09-01",
                "2026-12-31"
        );

        UUID sessionId = createManualSession(
                batchId,
                "2026-09-14",
                "10:00:00",
                "12:00:00",
                "REST APIs"
        );

        changeSessionStatus(
                sessionId,
                "CANCELLED"
        );

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
                                          "status": "PLANNED"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("PLANNED")
                );
    }

    @Test
    void preventsUpdatingCompletedSession()
            throws Exception {

        AcademicResources resources =
                createResources("LOCKED");

        UUID batchId = createBatch(
                "SES-LOCKED",
                resources,
                "2026-09-01",
                "2026-12-31"
        );

        UUID sessionId = createManualSession(
                batchId,
                "2026-09-15",
                "10:00:00",
                "12:00:00",
                "Completed topic"
        );

        changeBatchStatus(
                batchId,
                "OPEN_FOR_ENROLLMENT"
        );

        changeBatchStatus(
                batchId,
                "IN_PROGRESS"
        );

        changeSessionStatus(
                sessionId,
                "COMPLETED"
        );

        mockMvc.perform(
                        put(
                                SESSION_URL
                                        + "/"
                                        + sessionId
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        sessionRequest(
                                                "2026-09-15",
                                                "11:00:00",
                                                "13:00:00",
                                                "Changed topic"
                                        )
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Completed session configuration cannot be changed"
                                )
                );
    }

    private AcademicResources createResources(
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

        return new AcademicResources(
                branchId,
                classroomId,
                courseLevelId,
                instructorId
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
                                          "name": "Training Classroom",
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
                                          "description": "Session test course"
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
                                          "name": "Session Test Level",
                                          "sequenceNumber": 1,
                                          "durationHours": 60,
                                          "description": "Session test level"
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
                                          "bio": "Session test instructor"
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
            AcademicResources resources,
            String startDate,
            String endDate
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
                                          "startDate": "%s",
                                          "endDate": "%s"
                                        }
                                        """.formatted(
                                                code,
                                                code,
                                                resources.branchId(),
                                                resources.classroomId(),
                                                resources.courseLevelId(),
                                                resources.instructorId(),
                                                startDate,
                                                endDate
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    private UUID createWeeklySchedule(
            UUID batchId,
            String dayOfWeek,
            String startTime,
            String endTime
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post(
                                BATCHES_URL
                                        + "/"
                                        + batchId
                                        + "/schedules"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "dayOfWeek": "%s",
                                          "startTime": "%s",
                                          "endTime": "%s"
                                        }
                                        """.formatted(
                                                dayOfWeek,
                                                startTime,
                                                endTime
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    private UUID createManualSession(
            UUID batchId,
            String sessionDate,
            String startTime,
            String endTime,
            String topic
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
                                        sessionRequest(
                                                sessionDate,
                                                startTime,
                                                endTime,
                                                topic
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    private void changeBatchStatus(
            UUID batchId,
            String statusValue
    ) throws Exception {

        mockMvc.perform(
                        patch(
                                BATCHES_URL
                                        + "/"
                                        + batchId
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

    private String sessionRequest(
            String sessionDate,
            String startTime,
            String endTime,
            String topic
    ) {
        return """
                {
                  "sessionDate": "%s",
                  "startTime": "%s",
                  "endTime": "%s",
                  "topic": "%s"
                }
                """.formatted(
                sessionDate,
                startTime,
                endTime,
                topic
        );
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

    private record AcademicResources(
            UUID branchId,
            UUID classroomId,
            UUID courseLevelId,
            UUID instructorId
    ) {
    }
}