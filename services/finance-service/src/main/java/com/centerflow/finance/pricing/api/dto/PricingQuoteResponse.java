package com.centerflow.finance.pricing.api.dto;

import com.centerflow.finance.pricing.domain.PricingPlan;

import java.math.BigDecimal;
import java.util.UUID;

public record PricingQuoteResponse(

        UUID pricingPlanId,
        String pricingPlanCode,
        BigDecimal totalAmount,
        String currency,
        int installmentCount,
        BigDecimal initialPaymentAmount

) {

    public static PricingQuoteResponse from(
            PricingPlan pricingPlan
    ) {
        return new PricingQuoteResponse(
                pricingPlan.getId(),
                pricingPlan.getCode(),
                pricingPlan.getTotalAmount(),
                pricingPlan.getCurrency(),
                pricingPlan.getInstallmentCount(),
                pricingPlan.getInitialPaymentAmount()
        );
    }
}