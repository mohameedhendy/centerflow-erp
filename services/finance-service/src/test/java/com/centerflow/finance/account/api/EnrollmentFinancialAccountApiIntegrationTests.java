package com.centerflow.finance.account.api;

import com.centerflow.finance.pricing.domain.PricingPlan;
import com.centerflow.finance.pricing.repository.PricingPlanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EnrollmentFinancialAccountApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PricingPlanRepository pricingPlanRepository;

    @Test
    void createShouldSnapshotPlanAndGenerateInstallments()
            throws Exception {
        PricingPlan pricingPlan = savePlan(
                "ACCOUNT-PLAN",
                true
        );

        UUID enrollmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        String requestBody = """
                {
                  "enrollmentId": "%s",
                  "studentId": "%s",
                  "pricingPlanId": "%s",
                  "firstInstallmentDueDate": "2026-08-01"
                }
                """.formatted(
                enrollmentId,
                studentId,
                pricingPlan.getId()
        );

        mockMvc.perform(
                        post(
                                "/api/v1/finance/internal/"
                                        + "enrollment-accounts"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(
                        jsonPath("$.enrollmentId")
                                .value(enrollmentId.toString())
                )
                .andExpect(
                        jsonPath("$.studentId")
                                .value(studentId.toString())
                )
                .andExpect(
                        jsonPath("$.pricingPlanCode")
                                .value("ACCOUNT-PLAN")
                )
                .andExpect(
                        jsonPath("$.totalAmount")
                                .value(1000.00)
                )
                .andExpect(
                        jsonPath("$.initialPaymentAmount")
                                .value(300.00)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("OPEN")
                )
                .andExpect(
                        jsonPath(
                                "$.installments",
                                hasSize(3)
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.installments[0].amount"
                        ).value(333.33)
                )
                .andExpect(
                        jsonPath(
                                "$.installments[1].amount"
                        ).value(333.33)
                )
                .andExpect(
                        jsonPath(
                                "$.installments[2].amount"
                        ).value(333.34)
                )
                .andExpect(
                        jsonPath(
                                "$.installments[0].dueDate"
                        ).value("2026-08-01")
                )
                .andExpect(
                        jsonPath(
                                "$.installments[1].dueDate"
                        ).value("2026-09-01")
                )
                .andExpect(
                        jsonPath(
                                "$.installments[2].dueDate"
                        ).value("2026-10-01")
                )
                .andExpect(
                        jsonPath(
                                "$.installments[0].status"
                        ).value("PENDING")
                );
    }

    @Test
    void getShouldReturnAccountByEnrollmentId()
            throws Exception {
        PricingPlan pricingPlan = savePlan(
                "GET-ACCOUNT",
                true
        );

        UUID enrollmentId = UUID.randomUUID();

        createAccount(
                enrollmentId,
                UUID.randomUUID(),
                pricingPlan.getId()
        );

        mockMvc.perform(
                        get(
                                "/api/v1/finance/"
                                        + "enrollment-accounts/"
                                        + "{enrollmentId}",
                                enrollmentId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.enrollmentId")
                                .value(enrollmentId.toString())
                )
                .andExpect(
                        jsonPath("$.installments", hasSize(3))
                );
    }

    @Test
    void createShouldRejectDuplicateEnrollmentAccount()
            throws Exception {
        PricingPlan pricingPlan = savePlan(
                "DUPLICATE-ACCOUNT",
                true
        );

        UUID enrollmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        createAccount(
                enrollmentId,
                studentId,
                pricingPlan.getId()
        );

        String requestBody = requestBody(
                enrollmentId,
                studentId,
                pricingPlan.getId()
        );

        mockMvc.perform(
                        post(
                                "/api/v1/finance/internal/"
                                        + "enrollment-accounts"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Financial account already "
                                                + "exists for enrollment: "
                                                + enrollmentId
                                )
                );
    }

    @Test
    void createShouldRejectInactivePricingPlan()
            throws Exception {
        PricingPlan pricingPlan = savePlan(
                "INACTIVE-ACCOUNT",
                false
        );

        String requestBody = requestBody(
                UUID.randomUUID(),
                UUID.randomUUID(),
                pricingPlan.getId()
        );

        mockMvc.perform(
                        post(
                                "/api/v1/finance/internal/"
                                        + "enrollment-accounts"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Pricing plan is not active: "
                                                + pricingPlan.getId()
                                )
                );
    }

    @Test
    void createShouldReturn404ForUnknownPricingPlan()
            throws Exception {
        UUID pricingPlanId = UUID.randomUUID();

        String requestBody = requestBody(
                UUID.randomUUID(),
                UUID.randomUUID(),
                pricingPlanId
        );

        mockMvc.perform(
                        post(
                                "/api/v1/finance/internal/"
                                        + "enrollment-accounts"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void createShouldValidateRequiredFields()
            throws Exception {
        mockMvc.perform(
                        post(
                                "/api/v1/finance/internal/"
                                        + "enrollment-accounts"
                        )
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
                                "$.validationErrors.enrollmentId"
                        ).value("Enrollment ID is required")
                )
                .andExpect(
                        jsonPath(
                                "$.validationErrors.studentId"
                        ).value("Student ID is required")
                )
                .andExpect(
                        jsonPath(
                                "$.validationErrors.pricingPlanId"
                        ).value("Pricing plan ID is required")
                )
                .andExpect(
                        jsonPath(
                                "$.validationErrors."
                                        + "firstInstallmentDueDate"
                        ).value(
                                "First installment due date "
                                        + "is required"
                        )
                );
    }

    private void createAccount(
            UUID enrollmentId,
            UUID studentId,
            UUID pricingPlanId
    ) throws Exception {
        mockMvc.perform(
                        post(
                                "/api/v1/finance/internal/"
                                        + "enrollment-accounts"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        requestBody(
                                                enrollmentId,
                                                studentId,
                                                pricingPlanId
                                        )
                                )
                )
                .andExpect(status().isCreated());
    }

    private String requestBody(
            UUID enrollmentId,
            UUID studentId,
            UUID pricingPlanId
    ) {
        return """
                {
                  "enrollmentId": "%s",
                  "studentId": "%s",
                  "pricingPlanId": "%s",
                  "firstInstallmentDueDate": "2026-08-01"
                }
                """.formatted(
                enrollmentId,
                studentId,
                pricingPlanId
        );
    }

    private PricingPlan savePlan(
            String code,
            boolean active
    ) {
        PricingPlan pricingPlan = PricingPlan.create(
                code,
                "Enrollment Financial Plan",
                "Plan used for account generation",
                new BigDecimal("1000.00"),
                "EGP",
                3,
                new BigDecimal("300.00")
        );

        if (!active) {
            pricingPlan.deactivate();
        }

        return pricingPlanRepository.saveAndFlush(
                pricingPlan
        );
    }
}