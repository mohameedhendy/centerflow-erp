package com.centerflow.academic;

import com.centerflow.academic.course.repository.CourseRepository;
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
class CourseApiTests {

    private static final String COURSES_URL =
            "/api/v1/academic/courses";

    private final MockMvc mockMvc;
    private final CourseRepository courseRepository;

    @Autowired
    CourseApiTests(
            MockMvc mockMvc,
            CourseRepository courseRepository
    ) {
        this.mockMvc = mockMvc;
        this.courseRepository = courseRepository;
    }

    @Test
    void createsCourseAndNormalizesValues()
            throws Exception {

        mockMvc.perform(
                        post(COURSES_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "code": "java-backend",
                                          "name": " Java Backend Development ",
                                          "description": "Spring Boot backend track"
                                        }
                                        """
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.id").isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("JAVA-BACKEND")
                )
                .andExpect(
                        jsonPath("$.name")
                                .value(
                                        "Java Backend Development"
                                )
                )
                .andExpect(
                        jsonPath("$.active").value(true)
                );

        assertThat(
                courseRepository.existsByCode(
                        "JAVA-BACKEND"
                )
        ).isTrue();
    }

    @Test
    void rejectsDuplicateCourseCode()
            throws Exception {

        createCourse(
                "ENGLISH",
                "English Language"
        );

        mockMvc.perform(
                        post(COURSES_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "code": "english",
                                          "name": "Another English Course"
                                        }
                                        """
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "A course already exists with code: ENGLISH"
                                )
                );
    }

    @Test
    void updatesCourseAndChangesStatus()
            throws Exception {

        UUID courseId = createCourse(
                "ACCOUNTING",
                "Accounting"
        );

        mockMvc.perform(
                        put(
                                COURSES_URL
                                        + "/"
                                        + courseId
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "name": "Professional Accounting",
                                          "description": "Complete accounting track"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.code")
                                .value("ACCOUNTING")
                )
                .andExpect(
                        jsonPath("$.name")
                                .value(
                                        "Professional Accounting"
                                )
                );

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
    void searchesCoursesInDatabase()
            throws Exception {

        createCourse(
                "JAVA-01",
                "Java Backend"
        );

        createCourse(
                "JAVA-02",
                "Advanced Java"
        );

        createCourse(
                "ENGLISH-01",
                "English Language"
        );

        mockMvc.perform(
                        get(COURSES_URL)
                                .queryParam(
                                        "search",
                                        "java"
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
}