package com.centerflow.finance.pricing.exception;

import java.util.UUID;

public class PricingPlanNotFoundException
        extends RuntimeException {

    public PricingPlanNotFoundException(UUID pricingPlanId) {
        super("Pricing plan not found: " + pricingPlanId);
    }
}