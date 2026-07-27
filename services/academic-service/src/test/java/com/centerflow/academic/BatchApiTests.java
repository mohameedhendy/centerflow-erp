package com.centerflow.academic;

import com.centerflow.academic.batch.domain.Batch;
import com.centerflow.academic.batch.domain.BatchStatus;
import com.centerflow.academic.batch.repository.BatchRepository;
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
@Transactional
class BatchApiTests {

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

    private final MockMvc mockMvc;
    private final BatchRepository batchRepository;

    @Autowired
    BatchApiTests(
            MockMvc mockMvc,
            BatchRepository batchRepository
    ) {
        this.mockMvc = mockMvc;
        this.batchRepository = batchRepository;
    }

    @Test
    void createsBatchInDraftStatus()
            throws Exception {

        BatchResources resources =
                createBatchResources("CREATE");

        mockMvc.perform(
                        post(BATCHES_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        batchRequest(
                                                "java-2026-a",
                                                " Java Backend Morning Batch ",
                                                resources,
                                                25,
                                                "2026-09-01",
                                                "2026-12-31"
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.id").isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("JAVA-2026-A")
                )
                .andExpect(
                        jsonPath("$.name")
                                .value(
                                        "Java Backend Morning Batch"
                                )
                )
                .andExpect(
                        jsonPath("$.branchId")
                                .value(
                                        resources.branchId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.classroomId")
                                .value(
                                        resources.classroomId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.capacity")
                                .value(25)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("DRAFT")
                );

        assertThat(
                batchRepository.existsByCode(
                        "JAVA-2026-A"
                )
        ).isTrue();
    }

    @Test
    void rejectsDuplicateBatchCodeIgnoringCase()
            throws Exception {

        BatchResources resources =
                createBatchResources("DUPLICATE");

        createBatch(
                "BATCH-DUP-01",
                "First Batch",
                resources,
                20
        );

        mockMvc.perform(
                        post(BATCHES_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        batchRequest(
                                                "batch-dup-01",
                                                "Duplicate Batch",
                                                resources,
                                                20,
                                                "2026-09-01",
                                                "2026-12-31"
                                        )
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "A batch already exists with code: BATCH-DUP-01"
                                )
                );
    }

    @Test
    void rejectsClassroomFromDifferentBranch()
            throws Exception {

        UUID firstBranchId = createBranch(
                "BATCH-BR-A",
                "First Batch Branch"
        );

        UUID secondBranchId = createBranch(
                "BATCH-BR-B",
                "Second Batch Branch"
        );

        UUID classroomId = createClassroom(
                firstBranchId,
                "ROOM-A",
                40
        );

        UUID courseId = createCourse(
                "BATCH-COURSE-A",
                "Batch Course A"
        );

        UUID levelId = createCourseLevel(
                courseId,
                "LEVEL-A"
        );

        UUID instructorId = createInstructor(
                "INS-BATCH-A",
                "batch-a@centerflow.com"
        );

        BatchResources resources =
                new BatchResources(
                        secondBranchId,
                        classroomId,
                        levelId,
                        instructorId
                );

        mockMvc.perform(
                        post(BATCHES_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        batchRequest(
                                                "INVALID-BRANCH",
                                                "Invalid Branch Batch",
                                                resources,
                                                20,
                                                "2026-09-01",
                                                "2026-12-31"
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Classroom "
                                                + classroomId
                                                + " does not belong to branch "
                                                + secondBranchId
                                )
                );
    }

    @Test
    void rejectsCapacityAboveClassroomCapacity()
            throws Exception {

        BatchResources resources =
                createBatchResources(
                        "CAPACITY",
                        20
                );

        mockMvc.perform(
                        post(BATCHES_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        batchRequest(
                                                "BATCH-CAPACITY",
                                                "Oversized Batch",
                                                resources,
                                                30,
                                                "2026-09-01",
                                                "2026-12-31"
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Batch capacity 30 exceeds classroom capacity 20"
                                )
                );
    }

    @Test
    void changesStatusThroughValidLifecycle()
            throws Exception {

        BatchResources resources =
                createBatchResources("LIFECYCLE");

        UUID batchId = createBatch(
                "BATCH-LIFECYCLE",
                "Lifecycle Batch",
                resources,
                25
        );

        changeBatchStatus(
                batchId,
                BatchStatus.OPEN_FOR_ENROLLMENT
        );

        changeBatchStatus(
                batchId,
                BatchStatus.IN_PROGRESS
        );

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

        Batch batch = batchRepository
                .findById(batchId)
                .orElseThrow();

        assertThat(batch.getStatus())
                .isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    void rejectsInvalidStatusTransition()
            throws Exception {

        BatchResources resources =
                createBatchResources("STATUS");

        UUID batchId = createBatch(
                "BATCH-STATUS",
                "Invalid Status Batch",
                resources,
                20
        );

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
                                          "status": "COMPLETED"
                                        }
                                        """
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Batch status cannot change from DRAFT to COMPLETED"
                                )
                );
    }

    @Test
    void preventsConfigurationChangesAfterBatchStarts()
            throws Exception {

        BatchResources resources =
                createBatchResources("LOCKED");

        UUID batchId = createBatch(
                "BATCH-LOCKED",
                "Locked Batch",
                resources,
                20
        );

        changeBatchStatus(
                batchId,
                BatchStatus.OPEN_FOR_ENROLLMENT
        );

        changeBatchStatus(
                batchId,
                BatchStatus.IN_PROGRESS
        );

        mockMvc.perform(
                        put(
                                BATCHES_URL
                                        + "/"
                                        + batchId
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        updateBatchRequest(
                                                "Changed Batch",
                                                resources,
                                                15
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

    @Test
    void searchesAndFiltersBatchesInDatabase()
            throws Exception {

        BatchResources resources =
                createBatchResources("SEARCH");

        createBatch(
                "SPRING-MORNING",
                "Spring Boot Morning Batch",
                resources,
                25
        );

        createBatch(
                "ENGLISH-EVENING",
                "English Evening Batch",
                resources,
                20
        );

        mockMvc.perform(
                        get(BATCHES_URL)
                                .queryParam(
                                        "branchId",
                                        resources.branchId()
                                                .toString()
                                )
                                .queryParam(
                                        "courseLevelId",
                                        resources.courseLevelId()
                                                .toString()
                                )
                                .queryParam(
                                        "status",
                                        "DRAFT"
                                )
                                .queryParam(
                                        "search",
                                        "spring"
                                )
                                .queryParam(
                                        "startDateFrom",
                                        "2026-08-01"
                                )
                                .queryParam(
                                        "startDateTo",
                                        "2026-10-01"
                                )
                                .queryParam("page", "0")
                                .queryParam("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].code"
                        ).value("SPRING-MORNING")
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(1)
                );
    }

    private BatchResources createBatchResources(
            String suffix
    ) throws Exception {
        return createBatchResources(
                suffix,
                50
        );
    }

    private BatchResources createBatchResources(
            String suffix,
            int classroomCapacity
    ) throws Exception {

        UUID branchId = createBranch(
                "BR-" + suffix,
                suffix + " Branch"
        );

        UUID classroomId = createClassroom(
                branchId,
                "ROOM-" + suffix,
                classroomCapacity
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

        return new BatchResources(
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
                                          "name": "Main Classroom",
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
                                          "description": "Batch test course"
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
                                          "name": "Batch Test Level",
                                          "sequenceNumber": 1,
                                          "durationHours": 60,
                                          "description": "Batch test level"
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
                                          "bio": "Batch test instructor"
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
            String name,
            BatchResources resources,
            int capacity
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post(BATCHES_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        batchRequest(
                                                code,
                                                name,
                                                resources,
                                                capacity,
                                                "2026-09-01",
                                                "2026-12-31"
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    private void changeBatchStatus(
            UUID batchId,
            BatchStatus status
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
                                        """.formatted(status)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value(status.name())
                );
    }

    private String batchRequest(
            String code,
            String name,
            BatchResources resources,
            int capacity,
            String startDate,
            String endDate
    ) {
        return """
                {
                  "code": "%s",
                  "name": "%s",
                  "branchId": "%s",
                  "classroomId": "%s",
                  "courseLevelId": "%s",
                  "instructorId": "%s",
                  "capacity": %d,
                  "startDate": "%s",
                  "endDate": "%s"
                }
                """.formatted(
                code,
                name,
                resources.branchId(),
                resources.classroomId(),
                resources.courseLevelId(),
                resources.instructorId(),
                capacity,
                startDate,
                endDate
        );
    }

    private String updateBatchRequest(
            String name,
            BatchResources resources,
            int capacity
    ) {
        return """
                {
                  "name": "%s",
                  "branchId": "%s",
                  "classroomId": "%s",
                  "courseLevelId": "%s",
                  "instructorId": "%s",
                  "capacity": %d,
                  "startDate": "2026-09-01",
                  "endDate": "2026-12-31"
                }
                """.formatted(
                name,
                resources.branchId(),
                resources.classroomId(),
                resources.courseLevelId(),
                resources.instructorId(),
                capacity
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

    private record BatchResources(
            UUID branchId,
            UUID classroomId,
            UUID courseLevelId,
            UUID instructorId
    ) {
    }
}