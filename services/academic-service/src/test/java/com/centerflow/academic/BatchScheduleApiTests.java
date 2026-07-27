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
class BatchScheduleApiTests {

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

    private static final String SCHEDULES_URL =
            "/api/v1/academic/batch-schedules";

    private final MockMvc mockMvc;

    @Autowired
    BatchScheduleApiTests(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void createsAndSearchesBatchSchedule()
            throws Exception {

        AcademicResources resources =
                createResources("CREATE");

        UUID batchId = createBatch(
                "SCH-CREATE",
                resources,
                "2026-09-01",
                "2026-12-31"
        );

        UUID scheduleId = createSchedule(
                batchId,
                "MONDAY",
                "10:00:00",
                "12:00:00"
        );

        mockMvc.perform(
                        get(
                                BATCHES_URL
                                        + "/"
                                        + batchId
                                        + "/schedules"
                        )
                                .queryParam(
                                        "dayOfWeek",
                                        "MONDAY"
                                )
                                .queryParam(
                                        "active",
                                        "true"
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
                                .value(scheduleId.toString())
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].dayOfWeek"
                        ).value("MONDAY")
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].startTime"
                        ).value("10:00:00")
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].endTime"
                        ).value("12:00:00")
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(1)
                );
    }

    @Test
    void rejectsClassroomConflictForOverlappingBatches()
            throws Exception {

        AcademicResources firstResources =
                createResources("ROOM-CONFLICT");

        UUID secondInstructorId =
                createInstructor(
                        "INS-ROOM-CONFLICT-2",
                        "room-conflict-2@centerflow.com"
                );

        AcademicResources secondResources =
                new AcademicResources(
                        firstResources.branchId(),
                        firstResources.classroomId(),
                        firstResources.courseLevelId(),
                        secondInstructorId
                );

        UUID firstBatchId = createBatch(
                "SCH-ROOM-A",
                firstResources,
                "2026-09-01",
                "2026-12-31"
        );

        UUID secondBatchId = createBatch(
                "SCH-ROOM-B",
                secondResources,
                "2026-10-01",
                "2027-01-31"
        );

        createSchedule(
                firstBatchId,
                "MONDAY",
                "10:00:00",
                "12:00:00"
        );

        mockMvc.perform(
                        post(
                                BATCHES_URL
                                        + "/"
                                        + secondBatchId
                                        + "/schedules"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        scheduleRequest(
                                                "MONDAY",
                                                "11:00:00",
                                                "13:00:00"
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
                                                + " is already scheduled on MONDAY during the requested time"
                                )
                );
    }

    @Test
    void rejectsInstructorConflictAcrossDifferentClassrooms()
            throws Exception {

        AcademicResources firstResources =
                createResources("INS-CONFLICT");

        UUID secondClassroomId =
                createClassroom(
                        firstResources.branchId(),
                        "ROOM-INS-CONFLICT-2",
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
                "SCH-INS-A",
                firstResources,
                "2026-09-01",
                "2026-12-31"
        );

        UUID secondBatchId = createBatch(
                "SCH-INS-B",
                secondResources,
                "2026-09-01",
                "2026-12-31"
        );

        createSchedule(
                firstBatchId,
                "TUESDAY",
                "14:00:00",
                "16:00:00"
        );

        mockMvc.perform(
                        post(
                                BATCHES_URL
                                        + "/"
                                        + secondBatchId
                                        + "/schedules"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        scheduleRequest(
                                                "TUESDAY",
                                                "15:00:00",
                                                "17:00:00"
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
                                                + " is already scheduled on TUESDAY during the requested time"
                                )
                );
    }

    @Test
    void allowsBackToBackSchedules()
            throws Exception {

        AcademicResources resources =
                createResources("BACK-TO-BACK");

        UUID batchId = createBatch(
                "SCH-BACK",
                resources,
                "2026-09-01",
                "2026-12-31"
        );

        createSchedule(
                batchId,
                "WEDNESDAY",
                "10:00:00",
                "12:00:00"
        );

        mockMvc.perform(
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
                                        scheduleRequest(
                                                "WEDNESDAY",
                                                "12:00:00",
                                                "14:00:00"
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
    void allowsSameSlotForNonOverlappingBatchPeriods()
            throws Exception {

        AcademicResources resources =
                createResources("DATES");

        UUID firstBatchId = createBatch(
                "SCH-DATE-A",
                resources,
                "2026-01-01",
                "2026-03-31"
        );

        UUID secondBatchId = createBatch(
                "SCH-DATE-B",
                resources,
                "2026-04-01",
                "2026-06-30"
        );

        createSchedule(
                firstBatchId,
                "THURSDAY",
                "18:00:00",
                "20:00:00"
        );

        mockMvc.perform(
                        post(
                                BATCHES_URL
                                        + "/"
                                        + secondBatchId
                                        + "/schedules"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        scheduleRequest(
                                                "THURSDAY",
                                                "18:00:00",
                                                "20:00:00"
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.dayOfWeek")
                                .value("THURSDAY")
                );
    }

    @Test
    void updatesAndDeactivatesSchedule()
            throws Exception {

        AcademicResources resources =
                createResources("UPDATE");

        UUID batchId = createBatch(
                "SCH-UPDATE",
                resources,
                "2026-09-01",
                "2026-12-31"
        );

        UUID scheduleId = createSchedule(
                batchId,
                "SATURDAY",
                "09:00:00",
                "11:00:00"
        );

        mockMvc.perform(
                        put(
                                SCHEDULES_URL
                                        + "/"
                                        + scheduleId
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        scheduleRequest(
                                                "SUNDAY",
                                                "13:00:00",
                                                "15:00:00"
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.dayOfWeek")
                                .value("SUNDAY")
                )
                .andExpect(
                        jsonPath("$.startTime")
                                .value("13:00:00")
                )
                .andExpect(
                        jsonPath("$.endTime")
                                .value("15:00:00")
                );

        mockMvc.perform(
                        patch(
                                SCHEDULES_URL
                                        + "/"
                                        + scheduleId
                                        + "/status"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "active": false
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.active")
                                .value(false)
                );
    }

    @Test
    void rejectsInvalidScheduleTimeRange()
            throws Exception {

        AcademicResources resources =
                createResources("INVALID-TIME");

        UUID batchId = createBatch(
                "SCH-INVALID",
                resources,
                "2026-09-01",
                "2026-12-31"
        );

        mockMvc.perform(
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
                                        scheduleRequest(
                                                "FRIDAY",
                                                "14:00:00",
                                                "12:00:00"
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Schedule end time must be after start time"
                                )
                );
    }

    @Test
    void preventsScheduleChangesAfterBatchStarts()
            throws Exception {

        AcademicResources resources =
                createResources("LOCKED");

        UUID batchId = createBatch(
                "SCH-LOCKED",
                resources,
                "2026-09-01",
                "2026-12-31"
        );

        UUID scheduleId = createSchedule(
                batchId,
                "MONDAY",
                "10:00:00",
                "12:00:00"
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
                        put(
                                SCHEDULES_URL
                                        + "/"
                                        + scheduleId
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        scheduleRequest(
                                                "MONDAY",
                                                "11:00:00",
                                                "13:00:00"
                                        )
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Batch configuration cannot be changed while status is IN_PROGRESS"
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
                                          "description": "Schedule test course"
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
                                          "name": "Schedule Test Level",
                                          "sequenceNumber": 1,
                                          "durationHours": 60,
                                          "description": "Schedule test level"
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
                                          "bio": "Schedule test instructor"
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

    private UUID createSchedule(
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
                                        scheduleRequest(
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
                .andExpect(status().isOk());
    }

    private String scheduleRequest(
            String dayOfWeek,
            String startTime,
            String endTime
    ) {
        return """
                {
                  "dayOfWeek": "%s",
                  "startTime": "%s",
                  "endTime": "%s"
                }
                """.formatted(
                dayOfWeek,
                startTime,
                endTime
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