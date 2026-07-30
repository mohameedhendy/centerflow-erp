package com.centerflow.finance.pricing.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePricingPlanRequest(

        @NotBlank(message = "Pricing plan code is required")
        @Size(
                max = 30,
                message = "Pricing plan code must not exceed 30 characters"
        )
        String code,

        @NotBlank(message = "Pricing plan name is required")
        @Size(
                max = 150,
                message = "Pricing plan name must not exceed 150 characters"
        )
        String name,

        @Size(
                max = 500,
                message = "Pricing plan description must not exceed 500 characters"
        )
        String description,

        @NotNull(message = "Total amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Total amount must be greater than zero"
        )
        BigDecimal totalAmount,

        @NotBlank(message = "Currency is required")
        @Pattern(
                regexp = "[A-Za-z]{3}",
                message = "Currency must contain exactly three letters"
        )
        String currency,

        @NotNull(message = "Installment count is required")
        @Min(
                value = 1,
                message = "Installment count must be between 1 and 60"
        )
        @Max(
                value = 60,
                message = "Installment count must be between 1 and 60"
        )
        Integer installmentCount,

        @NotNull(message = "Initial payment amount is required")
        @DecimalMin(
                value = "0.00",
                message = "Initial payment amount cannot be negative"
        )
        BigDecimal initialPaymentAmount

) {
}