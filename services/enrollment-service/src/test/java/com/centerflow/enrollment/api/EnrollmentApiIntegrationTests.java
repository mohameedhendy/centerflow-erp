package com.centerflow.enrollment.api;

import com.centerflow.enrollment.domain.Enrollment;
import com.centerflow.enrollment.repository.EnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EnrollmentApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Test
    void createEnrollmentShouldReturn201() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();

        String requestBody = """
                {
                  "studentId": "%s",
                  "batchId": "%s"
                }
                """.formatted(studentId, batchId);

        mockMvc.perform(
                        post("/api/v1/enrollments")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        header().exists("Location")
                )
                .andExpect(
                        jsonPath("$.id").isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.enrollmentNumber")
                                .value(
                                        matchesPattern(
                                                "ENR-\\d{4}-\\d{6}"
                                        )
                                )
                )
                .andExpect(
                        jsonPath("$.studentId")
                                .value(studentId.toString())
                )
                .andExpect(
                        jsonPath("$.batchId")
                                .value(batchId.toString())
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("PENDING_PAYMENT")
                );
    }

    @Test
    void getEnrollmentShouldReturnSavedEnrollment()
            throws Exception {
        Enrollment enrollment = Enrollment.create(
                "ENR-2026-000100",
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        enrollmentRepository.saveAndFlush(enrollment);

        mockMvc.perform(
                        get(
                                "/api/v1/enrollments/{enrollmentId}",
                                enrollment.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        enrollment
                                                .getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.enrollmentNumber")
                                .value("ENR-2026-000100")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("PENDING_PAYMENT")
                );
    }

    @Test
    void createEnrollmentShouldReturn409ForDuplicate()
            throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();

        Enrollment enrollment = Enrollment.create(
                "ENR-2026-000101",
                studentId,
                batchId
        );

        enrollmentRepository.saveAndFlush(enrollment);

        String requestBody = """
                {
                  "studentId": "%s",
                  "batchId": "%s"
                }
                """.formatted(studentId, batchId);

        mockMvc.perform(
                        post("/api/v1/enrollments")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Student already has an open "
                                                + "enrollment in this batch"
                                )
                )
                .andExpect(
                        jsonPath("$.path")
                                .value("/api/v1/enrollments")
                );
    }

    @Test
    void createEnrollmentShouldReturn400ForMissingIds()
            throws Exception {
        mockMvc.perform(
                        post("/api/v1/enrollments")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("{}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value("Validation failed")
                )
                .andExpect(
                        jsonPath(
                                "$.validationErrors.studentId"
                        ).value("Student ID is required")
                )
                .andExpect(
                        jsonPath(
                                "$.validationErrors.batchId"
                        ).value("Batch ID is required")
                );
    }

    @Test
    void getEnrollmentShouldReturn404ForUnknownId()
            throws Exception {
        UUID enrollmentId = UUID.randomUUID();

        mockMvc.perform(
                        get(
                                "/api/v1/enrollments/{enrollmentId}",
                                enrollmentId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Enrollment was not found with ID: "
                                                + enrollmentId
                                )
                );
    }
}