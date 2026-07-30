package com.centerflow.finance.pricing.api;

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
class PricingPlanApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PricingPlanRepository pricingPlanRepository;

    @Test
    void createShouldReturn201AndNormalizedPlan()
            throws Exception {
        mockMvc.perform(
                        post("/api/v1/finance/pricing-plans")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "code": " monthly-4 ",
                                          "name": " Four Installments ",
                                          "description": " Flexible payment ",
                                          "totalAmount": 4000,
                                          "currency": "egp",
                                          "installmentCount": 4,
                                          "initialPaymentAmount": 1000
                                        }
                                        """
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(
                        jsonPath("$.code")
                                .value("MONTHLY-4")
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Four Installments")
                )
                .andExpect(
                        jsonPath("$.currency")
                                .value("EGP")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );
    }

    @Test
    void createShouldRejectDuplicateCode()
            throws Exception {
        savePlan("DUPLICATE-PLAN", "Existing Plan");

        mockMvc.perform(
                        post("/api/v1/finance/pricing-plans")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "code": "duplicate-plan",
                                          "name": "Duplicate Plan",
                                          "totalAmount": 2000,
                                          "currency": "EGP",
                                          "installmentCount": 2,
                                          "initialPaymentAmount": 1000
                                        }
                                        """
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Pricing plan code already "
                                                + "exists: DUPLICATE-PLAN"
                                )
                );
    }

    @Test
    void createShouldRejectInitialPaymentAboveTotal()
            throws Exception {
        mockMvc.perform(
                        post("/api/v1/finance/pricing-plans")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "code": "INVALID-AMOUNT",
                                          "name": "Invalid Amount",
                                          "totalAmount": 1000,
                                          "currency": "EGP",
                                          "installmentCount": 1,
                                          "initialPaymentAmount": 1000.01
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Initial payment amount "
                                                + "cannot exceed total amount"
                                )
                );
    }

    @Test
    void getShouldReturnSavedPricingPlan()
            throws Exception {
        PricingPlan pricingPlan =
                savePlan("GET-PLAN", "Get Plan");

        mockMvc.perform(
                        get(
                                "/api/v1/finance/pricing-plans/{id}",
                                pricingPlan.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        pricingPlan
                                                .getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("GET-PLAN")
                );
    }

    @Test
    void getShouldReturn404ForUnknownPlan()
            throws Exception {
        UUID pricingPlanId = UUID.randomUUID();

        mockMvc.perform(
                        get(
                                "/api/v1/finance/pricing-plans/{id}",
                                pricingPlanId
                        )
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void searchShouldFilterByCodeNameAndStatus()
            throws Exception {
        savePlan(
                "MONTHLY-SPECIAL",
                "Special Monthly Plan"
        );

        PricingPlan inactivePlan = savePlan(
                "MONTHLY-INACTIVE",
                "Special Monthly Old Plan"
        );

        inactivePlan.deactivate();
        pricingPlanRepository.flush();

        savePlan(
                "FULL-PAYMENT",
                "Full Payment Plan"
        );

        mockMvc.perform(
                        get("/api/v1/finance/pricing-plans")
                                .param("code", "monthly")
                                .param("name", "special")
                                .param("status", "ACTIVE")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content", hasSize(1))
                )
                .andExpect(
                        jsonPath("$.content[0].code")
                                .value("MONTHLY-SPECIAL")
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(1)
                );
    }

    @Test
    void statusCommandsShouldDeactivateAndActivatePlan()
            throws Exception {
        PricingPlan pricingPlan =
                savePlan("STATUS-PLAN", "Status Plan");

        mockMvc.perform(
                        post(
                                "/api/v1/finance/pricing-plans/"
                                        + "{id}/deactivate",
                                pricingPlan.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("INACTIVE")
                );

        mockMvc.perform(
                        post(
                                "/api/v1/finance/pricing-plans/"
                                        + "{id}/activate",
                                pricingPlan.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );
    }

    @Test
    void searchShouldRejectInvalidPagination()
            throws Exception {
        mockMvc.perform(
                        get("/api/v1/finance/pricing-plans")
                                .param("page", "-1")
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Page index must be zero "
                                                + "or greater"
                                )
                );
    }

    @Test
    void createShouldValidateRequiredFields()
            throws Exception {
        mockMvc.perform(
                        post("/api/v1/finance/pricing-plans")
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
                                "$.validationErrors.code"
                        ).value(
                                "Pricing plan code is required"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.validationErrors.name"
                        ).value(
                                "Pricing plan name is required"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.validationErrors.totalAmount"
                        ).value(
                                "Total amount is required"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.validationErrors.currency"
                        ).value(
                                "Currency is required"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.validationErrors.installmentCount"
                        ).value(
                                "Installment count is required"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.validationErrors.initialPaymentAmount"
                        ).value(
                                "Initial payment amount is required"
                        )
                );
    }

    private PricingPlan savePlan(
            String code,
            String name
    ) {
        PricingPlan pricingPlan = PricingPlan.create(
                code,
                name,
                "Pricing plan description",
                new BigDecimal("3000"),
                "EGP",
                3,
                new BigDecimal("1000")
        );

        return pricingPlanRepository.saveAndFlush(
                pricingPlan
        );
    }
}