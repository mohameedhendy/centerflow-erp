package com.centerflow.finance.pricing.repository;

import com.centerflow.finance.pricing.domain.PricingPlan;
import com.centerflow.finance.pricing.domain.PricingPlanStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PricingPlanRepositoryTests {

    @Autowired
    private PricingPlanRepository pricingPlanRepository;

    @Test
    void saveShouldPersistAndFindPlanByCode() {
        PricingPlan savedPlan = pricingPlanRepository.saveAndFlush(
                createPlan("MONTHLY-3")
        );

        PricingPlan foundPlan = pricingPlanRepository
                .findByCode("MONTHLY-3")
                .orElseThrow();

        assertThat(foundPlan.getId())
                .isEqualTo(savedPlan.getId());
        assertThat(foundPlan.getCode())
                .isEqualTo("MONTHLY-3");
        assertThat(foundPlan.getStatus())
                .isEqualTo(PricingPlanStatus.ACTIVE);
    }

    @Test
    void existsByCodeShouldReturnTrueForSavedPlan() {
        pricingPlanRepository.saveAndFlush(
                createPlan("FULL-PAYMENT")
        );

        assertThat(
                pricingPlanRepository.existsByCode(
                        "FULL-PAYMENT"
                )
        ).isTrue();
    }

    @Test
    void findAllByStatusShouldFilterInsideDatabase() {
        PricingPlan activePlan = createPlan("ACTIVE-PLAN");

        PricingPlan inactivePlan =
                createPlan("INACTIVE-PLAN");

        inactivePlan.deactivate();

        pricingPlanRepository.save(activePlan);
        pricingPlanRepository.save(inactivePlan);
        pricingPlanRepository.flush();

        Page<PricingPlan> activePlans =
                pricingPlanRepository.findAllByStatus(
                        PricingPlanStatus.ACTIVE,
                        PageRequest.of(0, 10)
                );

        assertThat(activePlans.getTotalElements())
                .isEqualTo(1);

        assertThat(activePlans.getContent())
                .extracting(PricingPlan::getCode)
                .containsExactly("ACTIVE-PLAN");
    }

    private PricingPlan createPlan(String code) {
        return PricingPlan.create(
                code,
                "Standard Plan",
                "Standard center pricing plan",
                new BigDecimal("3000"),
                "EGP",
                3,
                new BigDecimal("1000")
        );
    }
}