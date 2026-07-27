package com.centerflow.academic;

import com.centerflow.academic.classroom.domain.Classroom;
import com.centerflow.academic.classroom.repository.ClassroomRepository;
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
class ClassroomApiTests {

    private static final String BRANCHES_URL =
            "/api/v1/academic/branches";

    private static final String CLASSROOMS_URL =
            "/api/v1/academic/classrooms";

    private final MockMvc mockMvc;
    private final ClassroomRepository classroomRepository;

    @Autowired
    ClassroomApiTests(
            MockMvc mockMvc,
            ClassroomRepository classroomRepository
    ) {
        this.mockMvc = mockMvc;
        this.classroomRepository =
                classroomRepository;
    }

    @Test
    void createsClassroomAndNormalizesValues()
            throws Exception {

        UUID branchId = createBranch(
                "ZAG-CLASS",
                "Zagazig Classrooms Branch"
        );

        mockMvc.perform(
                        post(CLASSROOMS_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        classroomRequest(
                                                branchId,
                                                "room-101",
                                                " Main Classroom ",
                                                35,
                                                " First Floor "
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.id").isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.branchId")
                                .value(
                                        branchId.toString()
                                )
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("ROOM-101")
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Main Classroom")
                )
                .andExpect(
                        jsonPath("$.capacity")
                                .value(35)
                )
                .andExpect(
                        jsonPath("$.floor")
                                .value("First Floor")
                )
                .andExpect(
                        jsonPath("$.active")
                                .value(true)
                );

        assertThat(
                classroomRepository
                        .existsByBranchIdAndCode(
                                branchId,
                                "ROOM-101"
                        )
        ).isTrue();
    }

    @Test
    void rejectsDuplicateCodeWithinSameBranch()
            throws Exception {

        UUID branchId = createBranch(
                "CAIRO-CLASS",
                "Cairo Classrooms Branch"
        );

        createClassroom(
                branchId,
                "ROOM-201",
                "First Room",
                30
        );

        mockMvc.perform(
                        post(CLASSROOMS_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        classroomRequest(
                                                branchId,
                                                "room-201",
                                                "Duplicate Room",
                                                40,
                                                "Second Floor"
                                        )
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "A classroom already exists with code ROOM-201 in branch "
                                                + branchId
                                )
                );
    }

    @Test
    void allowsSameCodeInDifferentBranches()
            throws Exception {

        UUID firstBranchId = createBranch(
                "BRANCH-A",
                "First Branch"
        );

        UUID secondBranchId = createBranch(
                "BRANCH-B",
                "Second Branch"
        );

        createClassroom(
                firstBranchId,
                "ROOM-101",
                "First Branch Room",
                25
        );

        createClassroom(
                secondBranchId,
                "ROOM-101",
                "Second Branch Room",
                35
        );

        assertThat(
                classroomRepository
                        .existsByBranchIdAndCode(
                                firstBranchId,
                                "ROOM-101"
                        )
        ).isTrue();

        assertThat(
                classroomRepository
                        .existsByBranchIdAndCode(
                                secondBranchId,
                                "ROOM-101"
                        )
        ).isTrue();
    }

    @Test
    void rejectsClassroomForInactiveBranch()
            throws Exception {

        UUID branchId = createBranch(
                "INACTIVE-BR",
                "Inactive Branch"
        );

        changeBranchStatus(
                branchId,
                false
        );

        mockMvc.perform(
                        post(CLASSROOMS_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        classroomRequest(
                                                branchId,
                                                "ROOM-001",
                                                "Unavailable Room",
                                                20,
                                                null
                                        )
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Branch is inactive: "
                                                + branchId
                                )
                );
    }

    @Test
    void updatesClassroomAndChangesStatus()
            throws Exception {

        UUID branchId = createBranch(
                "MANS-CLASS",
                "Mansoura Classrooms Branch"
        );

        UUID classroomId = createClassroom(
                branchId,
                "HALL-01",
                "Small Hall",
                20
        );

        mockMvc.perform(
                        put(
                                CLASSROOMS_URL
                                        + "/"
                                        + classroomId
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "name": "Large Training Hall",
                                          "capacity": 60,
                                          "floor": "Ground Floor"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.code")
                                .value("HALL-01")
                )
                .andExpect(
                        jsonPath("$.name")
                                .value(
                                        "Large Training Hall"
                                )
                )
                .andExpect(
                        jsonPath("$.capacity")
                                .value(60)
                );

        mockMvc.perform(
                        patch(
                                CLASSROOMS_URL
                                        + "/"
                                        + classroomId
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

        Classroom classroom =
                classroomRepository
                        .findById(classroomId)
                        .orElseThrow();

        assertThat(classroom.getCapacity())
                .isEqualTo(60);

        assertThat(classroom.isActive())
                .isFalse();
    }

    @Test
    void searchesAndFiltersClassroomsInDatabase()
            throws Exception {

        UUID branchId = createBranch(
                "SEARCH-BR",
                "Search Branch"
        );

        createClassroom(
                branchId,
                "ROOM-20",
                "Small Classroom",
                20
        );

        createClassroom(
                branchId,
                "ROOM-40",
                "Medium Classroom",
                40
        );

        createClassroom(
                branchId,
                "LAB-80",
                "Computer Laboratory",
                80
        );

        mockMvc.perform(
                        get(CLASSROOMS_URL)
                                .queryParam(
                                        "branchId",
                                        branchId.toString()
                                )
                                .queryParam(
                                        "search",
                                        "room"
                                )
                                .queryParam(
                                        "minimumCapacity",
                                        "25"
                                )
                                .queryParam(
                                        "maximumCapacity",
                                        "60"
                                )
                                .queryParam(
                                        "active",
                                        "true"
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
                        ).value("ROOM-40")
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(1)
                );
    }

    @Test
    void rejectsInvalidCapacityRange()
            throws Exception {

        mockMvc.perform(
                        get(CLASSROOMS_URL)
                                .queryParam(
                                        "minimumCapacity",
                                        "100"
                                )
                                .queryParam(
                                        "maximumCapacity",
                                        "20"
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Minimum capacity must not exceed maximum capacity"
                                )
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
                                          "city": "Test City"
                                        }
                                        """.formatted(
                                                code,
                                                name
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(
                JsonPath.read(
                        result.getResponse()
                                .getContentAsString(),
                        "$.id"
                )
        );
    }

    private UUID createClassroom(
            UUID branchId,
            String code,
            String name,
            int capacity
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post(CLASSROOMS_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        classroomRequest(
                                                branchId,
                                                code,
                                                name,
                                                capacity,
                                                "First Floor"
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(
                JsonPath.read(
                        result.getResponse()
                                .getContentAsString(),
                        "$.id"
                )
        );
    }

    private void changeBranchStatus(
            UUID branchId,
            boolean active
    ) throws Exception {

        mockMvc.perform(
                        patch(
                                BRANCHES_URL
                                        + "/"
                                        + branchId
                                        + "/status"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "active": %s
                                        }
                                        """.formatted(active)
                                )
                )
                .andExpect(status().isOk());
    }

    private String classroomRequest(
            UUID branchId,
            String code,
            String name,
            int capacity,
            String floor
    ) {
        String floorValue = floor == null
                ? "null"
                : "\"" + floor + "\"";

        return """
                {
                  "branchId": "%s",
                  "code": "%s",
                  "name": "%s",
                  "capacity": %d,
                  "floor": %s
                }
                """.formatted(
                branchId,
                code,
                name,
                capacity,
                floorValue
        );
    }
}