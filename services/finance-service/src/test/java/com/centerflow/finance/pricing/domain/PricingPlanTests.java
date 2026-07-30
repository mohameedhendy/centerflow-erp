package com.centerflow.finance.pricing.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingPlanTests {

    @Test
    void createShouldNormalizeAndInitializeActivePlan() {
        PricingPlan pricingPlan = PricingPlan.create(
                " monthly-3 ",
                " Three Installments ",
                "  Payment over three installments  ",
                new BigDecimal("3000"),
                "egp",
                3,
                new BigDecimal("1000")
        );

        assertThat(pricingPlan.getId()).isNotNull();
        assertThat(pricingPlan.getCode())
                .isEqualTo("MONTHLY-3");
        assertThat(pricingPlan.getName())
                .isEqualTo("Three Installments");
        assertThat(pricingPlan.getDescription())
                .isEqualTo(
                        "Payment over three installments"
                );
        assertThat(pricingPlan.getTotalAmount())
                .isEqualByComparingTo("3000.00");
        assertThat(pricingPlan.getCurrency())
                .isEqualTo("EGP");
        assertThat(pricingPlan.getInstallmentCount())
                .isEqualTo(3);
        assertThat(pricingPlan.getInitialPaymentAmount())
                .isEqualByComparingTo("1000.00");
        assertThat(pricingPlan.getStatus())
                .isEqualTo(PricingPlanStatus.ACTIVE);
        assertThat(pricingPlan.getCreatedAt()).isNotNull();
        assertThat(pricingPlan.getUpdatedAt()).isNotNull();
    }

    @Test
    void blankDescriptionShouldBecomeNull() {
        PricingPlan pricingPlan = createPlan(" ");

        assertThat(pricingPlan.getDescription()).isNull();
    }

    @Test
    void totalAmountMustBeGreaterThanZero() {
        assertThatThrownBy(
                () -> PricingPlan.create(
                        "PLAN-1",
                        "Plan",
                        null,
                        BigDecimal.ZERO,
                        "EGP",
                        1,
                        BigDecimal.ZERO
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Total amount must be greater than zero"
                );
    }

    @Test
    void initialPaymentCannotExceedTotalAmount() {
        assertThatThrownBy(
                () -> PricingPlan.create(
                        "PLAN-1",
                        "Plan",
                        null,
                        new BigDecimal("1000"),
                        "EGP",
                        1,
                        new BigDecimal("1000.01")
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Initial payment amount cannot exceed "
                                + "total amount"
                );
    }

    @Test
    void moneyShouldNotHaveMoreThanTwoDecimalPlaces() {
        assertThatThrownBy(
                () -> PricingPlan.create(
                        "PLAN-1",
                        "Plan",
                        null,
                        new BigDecimal("1000.001"),
                        "EGP",
                        1,
                        BigDecimal.ZERO
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Total amount must have no more than "
                                + "two decimal places"
                );
    }

    @Test
    void installmentCountMustBeWithinAllowedRange() {
        assertThatThrownBy(
                () -> PricingPlan.create(
                        "PLAN-1",
                        "Plan",
                        null,
                        new BigDecimal("1000"),
                        "EGP",
                        61,
                        BigDecimal.ZERO
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Installment count must be between 1 and 60"
                );
    }

    @Test
    void currencyMustContainExactlyThreeLetters() {
        assertThatThrownBy(
                () -> PricingPlan.create(
                        "PLAN-1",
                        "Plan",
                        null,
                        new BigDecimal("1000"),
                        "EG",
                        1,
                        BigDecimal.ZERO
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Currency must contain exactly three letters"
                );
    }

    @Test
    void activateAndDeactivateShouldBeIdempotent() {
        PricingPlan pricingPlan = createPlan(null);

        pricingPlan.deactivate();
        pricingPlan.deactivate();

        assertThat(pricingPlan.getStatus())
                .isEqualTo(PricingPlanStatus.INACTIVE);

        pricingPlan.activate();
        pricingPlan.activate();

        assertThat(pricingPlan.getStatus())
                .isEqualTo(PricingPlanStatus.ACTIVE);
    }

    private PricingPlan createPlan(String description) {
        return PricingPlan.create(
                "PLAN-1",
                "Standard Plan",
                description,
                new BigDecimal("1500"),
                "EGP",
                3,
                new BigDecimal("500")
        );
    }
}