package com.centerflow.finance.pricing.domain;

public class InvalidPricingPlanException
        extends IllegalArgumentException {

    public InvalidPricingPlanException(String message) {
        super(message);
    }
}