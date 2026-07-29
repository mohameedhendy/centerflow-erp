package com.centerflow.enrollment.api;

import com.centerflow.enrollment.domain.Enrollment;
import com.centerflow.enrollment.domain.EnrollmentStatus;
import com.centerflow.enrollment.repository.EnrollmentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Autowired
    private EntityManager entityManager;

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
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
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
        Enrollment enrollment = saveEnrollment(
                "ENR-2026-000100"
        );

        mockMvc.perform(
                        get(
                                "/api/v1/enrollments/{enrollmentId}",
                                enrollment.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(enrollment.getId().toString())
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
    void statusWorkflowShouldPersistAllValidTransitions()
            throws Exception {
        Enrollment enrollment = saveEnrollment(
                "ENR-2026-000101"
        );

        UUID enrollmentId = enrollment.getId();

        mockMvc.perform(
                        post(
                                "/api/v1/enrollments/internal/"
                                        + "{enrollmentId}/activate",
                                enrollmentId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status").value("ACTIVE")
                );

        mockMvc.perform(
                        post(
                                "/api/v1/enrollments/"
                                        + "{enrollmentId}/suspend",
                                enrollmentId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status").value("SUSPENDED")
                );

        mockMvc.perform(
                        post(
                                "/api/v1/enrollments/"
                                        + "{enrollmentId}/resume",
                                enrollmentId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status").value("ACTIVE")
                );

        mockMvc.perform(
                        post(
                                "/api/v1/enrollments/"
                                        + "{enrollmentId}/complete",
                                enrollmentId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status").value("COMPLETED")
                );

        entityManager.flush();
        entityManager.clear();

        Enrollment persistedEnrollment =
                enrollmentRepository.findById(enrollmentId)
                        .orElseThrow();

        assertThat(persistedEnrollment.getStatus())
                .isEqualTo(EnrollmentStatus.COMPLETED);
    }

    @Test
    void cancelShouldChangePendingEnrollmentToCancelled()
            throws Exception {
        Enrollment enrollment = saveEnrollment(
                "ENR-2026-000102"
        );

        mockMvc.perform(
                        post(
                                "/api/v1/enrollments/"
                                        + "{enrollmentId}/cancel",
                                enrollment.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status").value("CANCELLED")
                );

        mockMvc.perform(
                        post(
                                "/api/v1/enrollments/"
                                        + "{enrollmentId}/cancel",
                                enrollment.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status").value("CANCELLED")
                );
    }

    @Test
    void invalidStatusTransitionShouldReturn409()
            throws Exception {
        Enrollment enrollment = saveEnrollment(
                "ENR-2026-000103"
        );

        mockMvc.perform(
                        post(
                                "/api/v1/enrollments/"
                                        + "{enrollmentId}/suspend",
                                enrollment.getId()
                        )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Enrollment status cannot "
                                                + "change from "
                                                + "PENDING_PAYMENT "
                                                + "to SUSPENDED"
                                )
                );
    }

    @Test
    void completedEnrollmentShouldNotBeCancelled()
            throws Exception {
        Enrollment enrollment = saveEnrollment(
                "ENR-2026-000104"
        );

        enrollment.activate();
        enrollment.complete();
        enrollmentRepository.flush();

        mockMvc.perform(
                        post(
                                "/api/v1/enrollments/"
                                        + "{enrollmentId}/cancel",
                                enrollment.getId()
                        )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Enrollment status cannot "
                                                + "change from "
                                                + "COMPLETED "
                                                + "to CANCELLED"
                                )
                );
    }

    @Test
    void createEnrollmentShouldReturn409ForDuplicate()
            throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();

        Enrollment enrollment = Enrollment.create(
                "ENR-2026-000105",
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
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isConflict());
    }

    @Test
    void createEnrollmentShouldReturn400ForMissingIds()
            throws Exception {
        mockMvc.perform(
                        post("/api/v1/enrollments")
                                .contentType(MediaType.APPLICATION_JSON)
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
                .andExpect(status().isNotFound());
    }

    private Enrollment saveEnrollment(
            String enrollmentNumber
    ) {
        Enrollment enrollment = Enrollment.create(
                enrollmentNumber,
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        return enrollmentRepository.saveAndFlush(enrollment);
    }
}