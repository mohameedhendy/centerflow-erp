package com.centerflow.enrollment.api;

import com.centerflow.enrollment.domain.Enrollment;
import com.centerflow.enrollment.repository.EnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EnrollmentPaymentActivationApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Test
    void paymentActivationShouldBeIdempotent()
            throws Exception {
        Enrollment enrollment =
                enrollmentRepository.saveAndFlush(
                        Enrollment.create(
                                "ENR-2026-999901",
                                UUID.randomUUID(),
                                UUID.randomUUID()
                        )
                );

        String activationPath =
                "/api/v1/enrollments/internal/"
                        + enrollment.getId()
                        + "/activate";

        mockMvc.perform(post(activationPath))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );

        mockMvc.perform(post(activationPath))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );
    }
}