package com.centerflow.finance.pricing.api.dto;

import com.centerflow.finance.pricing.domain.PricingPlan;
import com.centerflow.finance.pricing.domain.PricingPlanStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PricingPlanResponse(

        UUID id,
        String code,
        String name,
        String description,
        BigDecimal totalAmount,
        String currency,
        int installmentCount,
        BigDecimal initialPaymentAmount,
        PricingPlanStatus status,
        Instant createdAt,
        Instant updatedAt

) {

    public static PricingPlanResponse from(
            PricingPlan pricingPlan
    ) {
        return new PricingPlanResponse(
                pricingPlan.getId(),
                pricingPlan.getCode(),
                pricingPlan.getName(),
                pricingPlan.getDescription(),
                pricingPlan.getTotalAmount(),
                pricingPlan.getCurrency(),
                pricingPlan.getInstallmentCount(),
                pricingPlan.getInitialPaymentAmount(),
                pricingPlan.getStatus(),
                pricingPlan.getCreatedAt(),
                pricingPlan.getUpdatedAt()
        );
    }
}