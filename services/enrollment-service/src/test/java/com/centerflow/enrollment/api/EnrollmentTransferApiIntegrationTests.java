package com.centerflow.enrollment.api;

import com.centerflow.enrollment.domain.Enrollment;
import com.centerflow.enrollment.repository.EnrollmentRepository;
import com.centerflow.enrollment.repository.EnrollmentTransferRepository;
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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EnrollmentTransferApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private EnrollmentTransferRepository transferRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void transferShouldUpdateBatchAndCreateHistory()
            throws Exception {
        UUID sourceBatchId = UUID.randomUUID();
        UUID targetBatchId = UUID.randomUUID();

        Enrollment enrollment = createEnrollment(
                "ENR-2026-020001",
                UUID.randomUUID(),
                sourceBatchId,
                true
        );

        String body = """
                {
                  "targetBatchId": "%s",
                  "reason": "Student requested another schedule"
                }
                """.formatted(targetBatchId);

        mockMvc.perform(
                        post(
                                "/api/v1/enrollments/{id}/transfers",
                                enrollment.getId()
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.fromBatchId")
                                .value(sourceBatchId.toString())
                )
                .andExpect(
                        jsonPath("$.toBatchId")
                                .value(targetBatchId.toString())
                )
                .andExpect(
                        jsonPath("$.reason")
                                .value(
                                        "Student requested "
                                                + "another schedule"
                                )
                );

        entityManager.flush();
        entityManager.clear();

        Enrollment updatedEnrollment =
                enrollmentRepository
                        .findById(enrollment.getId())
                        .orElseThrow();

        assertThat(updatedEnrollment.getBatchId())
                .isEqualTo(targetBatchId);

        assertThat(
                transferRepository
                        .findAll()
        ).hasSize(1);
    }

    @Test
    void historyShouldReturnEnrollmentTransfers()
            throws Exception {
        UUID targetBatchId = UUID.randomUUID();

        Enrollment enrollment = createEnrollment(
                "ENR-2026-020002",
                UUID.randomUUID(),
                UUID.randomUUID(),
                true
        );

        String body = """
                {
                  "targetBatchId": "%s",
                  "reason": "Branch transfer"
                }
                """.formatted(targetBatchId);

        mockMvc.perform(
                post(
                        "/api/v1/enrollments/{id}/transfers",
                        enrollment.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        ).andExpect(status().isOk());

        mockMvc.perform(
                        get(
                                "/api/v1/enrollments/{id}/transfers",
                                enrollment.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content", hasSize(1))
                )
                .andExpect(
                        jsonPath("$.content[0].toBatchId")
                                .value(targetBatchId.toString())
                )
                .andExpect(
                        jsonPath("$.totalElements").value(1)
                );
    }

    @Test
    void transferShouldRejectPendingPaymentEnrollment()
            throws Exception {
        Enrollment enrollment = createEnrollment(
                "ENR-2026-020003",
                UUID.randomUUID(),
                UUID.randomUUID(),
                false
        );

        String body = """
                {
                  "targetBatchId": "%s",
                  "reason": "Schedule change"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(
                        post(
                                "/api/v1/enrollments/{id}/transfers",
                                enrollment.getId()
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Only active enrollment "
                                                + "can be transferred"
                                )
                );
    }

    @Test
    void transferShouldRejectCurrentBatch()
            throws Exception {
        UUID batchId = UUID.randomUUID();

        Enrollment enrollment = createEnrollment(
                "ENR-2026-020004",
                UUID.randomUUID(),
                batchId,
                true
        );

        String body = """
                {
                  "targetBatchId": "%s",
                  "reason": "Invalid transfer"
                }
                """.formatted(batchId);

        mockMvc.perform(
                        post(
                                "/api/v1/enrollments/{id}/transfers",
                                enrollment.getId()
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Enrollment is already assigned "
                                                + "to the target batch"
                                )
                );
    }

    @Test
    void transferShouldRejectExistingTargetEnrollment()
            throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID sourceBatchId = UUID.randomUUID();
        UUID targetBatchId = UUID.randomUUID();

        Enrollment sourceEnrollment = createEnrollment(
                "ENR-2026-020005",
                studentId,
                sourceBatchId,
                true
        );

        createEnrollment(
                "ENR-2026-020006",
                studentId,
                targetBatchId,
                false
        );

        String body = """
                {
                  "targetBatchId": "%s",
                  "reason": "Duplicate target"
                }
                """.formatted(targetBatchId);

        mockMvc.perform(
                        post(
                                "/api/v1/enrollments/{id}/transfers",
                                sourceEnrollment.getId()
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Student already has an open "
                                                + "enrollment in the "
                                                + "target batch"
                                )
                );
    }

    @Test
    void transferShouldValidateRequest()
            throws Exception {
        Enrollment enrollment = createEnrollment(
                "ENR-2026-020007",
                UUID.randomUUID(),
                UUID.randomUUID(),
                true
        );

        mockMvc.perform(
                        post(
                                "/api/v1/enrollments/{id}/transfers",
                                enrollment.getId()
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "reason": " "
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath(
                                "$.validationErrors.targetBatchId"
                        ).value("Target batch ID is required")
                )
                .andExpect(
                        jsonPath(
                                "$.validationErrors.reason"
                        ).value("Transfer reason is required")
                );
    }

    private Enrollment createEnrollment(
            String number,
            UUID studentId,
            UUID batchId,
            boolean active
    ) {
        Enrollment enrollment = Enrollment.create(
                number,
                studentId,
                batchId
        );

        if (active) {
            enrollment.activate();
        }

        return enrollmentRepository.saveAndFlush(enrollment);
    }
}