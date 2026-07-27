package com.centerflow.academic;

import com.centerflow.academic.instructor.domain.Instructor;
import com.centerflow.academic.instructor.repository.InstructorRepository;
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
class InstructorApiTests {

    private static final String INSTRUCTORS_URL =
            "/api/v1/academic/instructors";

    private final MockMvc mockMvc;
    private final InstructorRepository instructorRepository;

    @Autowired
    InstructorApiTests(
            MockMvc mockMvc,
            InstructorRepository instructorRepository
    ) {
        this.mockMvc = mockMvc;
        this.instructorRepository =
                instructorRepository;
    }

    @Test
    void createsInstructorAndNormalizesValues()
            throws Exception {

        mockMvc.perform(
                        post(INSTRUCTORS_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "code": "ins-001",
                                          "firstName": " Mohamed ",
                                          "lastName": " Ahmed ",
                                          "email": "MOHAMED@CENTERFLOW.COM",
                                          "phone": "+20 100 000 0000",
                                          "specialization": " Java Backend ",
                                          "bio": "Spring Boot instructor"
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
                                .value("INS-001")
                )
                .andExpect(
                        jsonPath("$.firstName")
                                .value("Mohamed")
                )
                .andExpect(
                        jsonPath("$.lastName")
                                .value("Ahmed")
                )
                .andExpect(
                        jsonPath("$.email")
                                .value(
                                        "mohamed@centerflow.com"
                                )
                )
                .andExpect(
                        jsonPath("$.specialization")
                                .value("Java Backend")
                )
                .andExpect(
                        jsonPath("$.active")
                                .value(true)
                );
    }

    @Test
    void rejectsDuplicateInstructorCode()
            throws Exception {

        createInstructor(
                "INS-100",
                "Ahmed",
                "Ali",
                "ahmed100@centerflow.com",
                "English"
        );

        mockMvc.perform(
                        post(INSTRUCTORS_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        instructorRequest(
                                                "ins-100",
                                                "Other",
                                                "Instructor",
                                                "other@centerflow.com",
                                                "Accounting"
                                        )
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "An instructor already exists with code: INS-100"
                                )
                );
    }

    @Test
    void rejectsDuplicateInstructorEmail()
            throws Exception {

        createInstructor(
                "INS-200",
                "Mona",
                "Hassan",
                "mona@centerflow.com",
                "English"
        );

        mockMvc.perform(
                        post(INSTRUCTORS_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        instructorRequest(
                                                "INS-201",
                                                "Mona",
                                                "Other",
                                                "MONA@CENTERFLOW.COM",
                                                "Accounting"
                                        )
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "An instructor already exists with email: mona@centerflow.com"
                                )
                );
    }

    @Test
    void updatesInstructorAndChangesStatus()
            throws Exception {

        UUID instructorId = createInstructor(
                "INS-300",
                "Ali",
                "Mahmoud",
                "ali@centerflow.com",
                "Java"
        );

        mockMvc.perform(
                        put(
                                INSTRUCTORS_URL
                                        + "/"
                                        + instructorId
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "firstName": "Ali",
                                          "lastName": "Mahmoud",
                                          "email": "ali.updated@centerflow.com",
                                          "phone": "+20 101 111 1111",
                                          "specialization": "Java Backend",
                                          "bio": "Senior Spring Boot instructor"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.code")
                                .value("INS-300")
                )
                .andExpect(
                        jsonPath("$.email")
                                .value(
                                        "ali.updated@centerflow.com"
                                )
                )
                .andExpect(
                        jsonPath("$.specialization")
                                .value("Java Backend")
                );

        mockMvc.perform(
                        patch(
                                INSTRUCTORS_URL
                                        + "/"
                                        + instructorId
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

        Instructor instructor =
                instructorRepository
                        .findById(instructorId)
                        .orElseThrow();

        assertThat(instructor.isActive())
                .isFalse();
    }

    @Test
    void searchesAndFiltersInstructorsInDatabase()
            throws Exception {

        createInstructor(
                "INS-JAVA-1",
                "Ahmed",
                "Mohamed",
                "java1@centerflow.com",
                "Java Backend"
        );

        createInstructor(
                "INS-JAVA-2",
                "Mona",
                "Ali",
                "java2@centerflow.com",
                "Java Backend"
        );

        createInstructor(
                "INS-ENG-1",
                "Sara",
                "Hassan",
                "english@centerflow.com",
                "English"
        );

        mockMvc.perform(
                        get(INSTRUCTORS_URL)
                                .queryParam(
                                        "search",
                                        "java"
                                )
                                .queryParam(
                                        "specialization",
                                        "JAVA BACKEND"
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

    @Test
    void returnsNotFoundForMissingInstructor()
            throws Exception {

        UUID instructorId = UUID.randomUUID();

        mockMvc.perform(
                        get(
                                INSTRUCTORS_URL
                                        + "/"
                                        + instructorId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Instructor was not found: "
                                                + instructorId
                                )
                );
    }

    @Test
    void rejectsInvalidInstructorRequest()
            throws Exception {

        mockMvc.perform(
                        post(INSTRUCTORS_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "code": "invalid code",
                                          "firstName": "",
                                          "lastName": "",
                                          "email": "invalid-email"
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value("Validation failed")
                )
                .andExpect(
                        jsonPath(
                                "$.validationErrors.code"
                        ).exists()
                )
                .andExpect(
                        jsonPath(
                                "$.validationErrors.firstName"
                        ).exists()
                )
                .andExpect(
                        jsonPath(
                                "$.validationErrors.lastName"
                        ).exists()
                )
                .andExpect(
                        jsonPath(
                                "$.validationErrors.email"
                        ).exists()
                );
    }

    private UUID createInstructor(
            String code,
            String firstName,
            String lastName,
            String email,
            String specialization
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post(INSTRUCTORS_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        instructorRequest(
                                                code,
                                                firstName,
                                                lastName,
                                                email,
                                                specialization
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

    private String instructorRequest(
            String code,
            String firstName,
            String lastName,
            String email,
            String specialization
    ) {
        return """
                {
                  "code": "%s",
                  "firstName": "%s",
                  "lastName": "%s",
                  "email": "%s",
                  "phone": "+20 100 000 0000",
                  "specialization": "%s",
                  "bio": "Test instructor"
                }
                """.formatted(
                code,
                firstName,
                lastName,
                email,
                specialization
        );
    }
}