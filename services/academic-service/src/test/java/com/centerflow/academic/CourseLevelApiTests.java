package com.centerflow.academic;

import com.centerflow.academic.courselevel.repository.CourseLevelRepository;
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
class CourseLevelApiTests {

    private static final String COURSES_URL =
            "/api/v1/academic/courses";

    private static final String LEVELS_URL =
            "/api/v1/academic/course-levels";

    private final MockMvc mockMvc;
    private final CourseLevelRepository levelRepository;

    @Autowired
    CourseLevelApiTests(
            MockMvc mockMvc,
            CourseLevelRepository levelRepository
    ) {
        this.mockMvc = mockMvc;
        this.levelRepository = levelRepository;
    }

    @Test
    void createsCourseLevelAndNormalizesValues()
            throws Exception {

        UUID courseId = createCourse(
                "JAVA-TRACK",
                "Java Track"
        );

        mockMvc.perform(
                        post(LEVELS_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        levelRequest(
                                                courseId,
                                                "level-1",
                                                " Java Fundamentals ",
                                                1,
                                                60
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.courseId")
                                .value(
                                        courseId.toString()
                                )
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("LEVEL-1")
                )
                .andExpect(
                        jsonPath("$.name")
                                .value(
                                        "Java Fundamentals"
                                )
                )
                .andExpect(
                        jsonPath("$.sequenceNumber")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.durationHours")
                                .value(60)
                )
                .andExpect(
                        jsonPath("$.active")
                                .value(true)
                );
    }

    @Test
    void rejectsDuplicateCodeOrSequenceWithinCourse()
            throws Exception {

        UUID courseId = createCourse(
                "WEB-TRACK",
                "Web Development"
        );

        createLevel(
                courseId,
                "LEVEL-1",
                "Web Fundamentals",
                1,
                40
        );

        mockMvc.perform(
                        post(LEVELS_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        levelRequest(
                                                courseId,
                                                "LEVEL-2",
                                                "Duplicate Sequence",
                                                1,
                                                50
                                        )
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "A course level already exists with code LEVEL-2 or sequence 1 in course "
                                                + courseId
                                )
                );
    }

    @Test
    void allowsSameLevelCodeInDifferentCourses()
            throws Exception {

        UUID firstCourseId = createCourse(
                "JAVA-A",
                "Java Course"
        );

        UUID secondCourseId = createCourse(
                "ENGLISH-A",
                "English Course"
        );

        createLevel(
                firstCourseId,
                "LEVEL-1",
                "Java Level One",
                1,
                50
        );

        createLevel(
                secondCourseId,
                "LEVEL-1",
                "English Level One",
                1,
                40
        );

        assertThat(
                levelRepository
                        .existsByCourseIdAndCode(
                                firstCourseId,
                                "LEVEL-1"
                        )
        ).isTrue();

        assertThat(
                levelRepository
                        .existsByCourseIdAndCode(
                                secondCourseId,
                                "LEVEL-1"
                        )
        ).isTrue();
    }

    @Test
    void rejectsLevelForInactiveCourse()
            throws Exception {

        UUID courseId = createCourse(
                "INACTIVE-COURSE",
                "Inactive Course"
        );

        changeCourseStatus(
                courseId,
                false
        );

        mockMvc.perform(
                        post(LEVELS_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        levelRequest(
                                                courseId,
                                                "LEVEL-1",
                                                "Unavailable Level",
                                                1,
                                                30
                                        )
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Course is inactive: "
                                                + courseId
                                )
                );
    }

    @Test
    void updatesLevelAndChangesStatus()
            throws Exception {

        UUID courseId = createCourse(
                "DESIGN-TRACK",
                "Design Track"
        );

        UUID levelId = createLevel(
                courseId,
                "LEVEL-1",
                "Design Basics",
                1,
                30
        );

        mockMvc.perform(
                        put(
                                LEVELS_URL
                                        + "/"
                                        + levelId
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "name": "Professional Design Basics",
                                          "sequenceNumber": 2,
                                          "durationHours": 45,
                                          "description": "Updated level"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.code")
                                .value("LEVEL-1")
                )
                .andExpect(
                        jsonPath("$.sequenceNumber")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.durationHours")
                                .value(45)
                );

        mockMvc.perform(
                        patch(
                                LEVELS_URL
                                        + "/"
                                        + levelId
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
    void searchesLevelsByCourseInDatabase()
            throws Exception {

        UUID courseId = createCourse(
                "SEARCH-COURSE",
                "Search Course"
        );

        createLevel(
                courseId,
                "LEVEL-1",
                "Fundamentals",
                1,
                30
        );

        createLevel(
                courseId,
                "LEVEL-2",
                "Advanced Fundamentals",
                2,
                50
        );

        mockMvc.perform(
                        get(LEVELS_URL)
                                .queryParam(
                                        "courseId",
                                        courseId.toString()
                                )
                                .queryParam(
                                        "search",
                                        "level"
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
                                .value(2)
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].sequenceNumber"
                        ).value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.content[1].sequenceNumber"
                        ).value(2)
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(2)
                );
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
                                          "description": "Test course"
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

    private UUID createLevel(
            UUID courseId,
            String code,
            String name,
            int sequenceNumber,
            int durationHours
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post(LEVELS_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        levelRequest(
                                                courseId,
                                                code,
                                                name,
                                                sequenceNumber,
                                                durationHours
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

    private void changeCourseStatus(
            UUID courseId,
            boolean active
    ) throws Exception {

        mockMvc.perform(
                        patch(
                                COURSES_URL
                                        + "/"
                                        + courseId
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

    private String levelRequest(
            UUID courseId,
            String code,
            String name,
            int sequenceNumber,
            int durationHours
    ) {
        return """
                {
                  "courseId": "%s",
                  "code": "%s",
                  "name": "%s",
                  "sequenceNumber": %d,
                  "durationHours": %d,
                  "description": "Test level"
                }
                """.formatted(
                courseId,
                code,
                name,
                sequenceNumber,
                durationHours
        );
    }
}