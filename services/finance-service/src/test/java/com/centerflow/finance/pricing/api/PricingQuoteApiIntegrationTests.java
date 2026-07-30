package com.centerflow.finance.pricing.api;

import com.centerflow.finance.pricing.domain.PricingPlan;
import com.centerflow.finance.pricing.repository.PricingPlanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PricingQuoteApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PricingPlanRepository pricingPlanRepository;

    @Test
    void activePlanShouldReturnPricingQuote()
            throws Exception {
        PricingPlan pricingPlan = savePlan(
                "QUOTE-ACTIVE",
                true
        );

        mockMvc.perform(
                        get(
                                "/api/v1/finance/internal/"
                                        + "pricing-quotes/{pricingPlanId}",
                                pricingPlan.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.pricingPlanId")
                                .value(
                                        pricingPlan
                                                .getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.pricingPlanCode")
                                .value("QUOTE-ACTIVE")
                )
                .andExpect(
                        jsonPath("$.totalAmount")
                                .value(3000.00)
                )
                .andExpect(
                        jsonPath("$.currency")
                                .value("EGP")
                )
                .andExpect(
                        jsonPath("$.installmentCount")
                                .value(3)
                )
                .andExpect(
                        jsonPath("$.initialPaymentAmount")
                                .value(1000.00)
                );
    }

    @Test
    void inactivePlanShouldReturn409()
            throws Exception {
        PricingPlan pricingPlan = savePlan(
                "QUOTE-INACTIVE",
                false
        );

        mockMvc.perform(
                        get(
                                "/api/v1/finance/internal/"
                                        + "pricing-quotes/{pricingPlanId}",
                                pricingPlan.getId()
                        )
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
    void unknownPlanShouldReturn404()
            throws Exception {
        UUID pricingPlanId = UUID.randomUUID();

        mockMvc.perform(
                        get(
                                "/api/v1/finance/internal/"
                                        + "pricing-quotes/{pricingPlanId}",
                                pricingPlanId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Pricing plan not found: "
                                                + pricingPlanId
                                )
                );
    }

    private PricingPlan savePlan(
            String code,
            boolean active
    ) {
        PricingPlan pricingPlan = PricingPlan.create(
                code,
                "Enrollment Pricing Plan",
                "Pricing plan used during enrollment",
                new BigDecimal("3000"),
                "EGP",
                3,
                new BigDecimal("1000")
        );

        if (!active) {
            pricingPlan.deactivate();
        }

        return pricingPlanRepository.saveAndFlush(
                pricingPlan
        );
    }
}