package com.centerflow.finance.adjustment.api.dto;

import com.centerflow.finance.adjustment.domain.FinancialAdjustmentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RecordFinancialAdjustmentRequest(

        @NotNull(
                message = "Adjustment type is required"
        )
        FinancialAdjustmentType type,

        @NotNull(
                message = "Adjustment amount is required"
        )
        @DecimalMin(
                value = "0.01",
                message = "Adjustment amount must be "
                        + "greater than zero"
        )
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Adjustment amount must have "
                        + "no more than two decimal places"
        )
        BigDecimal amount,

        @NotBlank(
                message = "Adjustment reason is required"
        )
        @Size(
                min = 3,
                max = 500,
                message = "Adjustment reason must be "
                        + "between 3 and 500 characters"
        )
        String reason,

        @Size(
                max = 100,
                message = "External reference must not "
                        + "exceed 100 characters"
        )
        String externalReference

) {
}