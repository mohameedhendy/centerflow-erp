package com.centerflow.finance.refund.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RecordRefundRequest(

        @NotNull(message = "Refund amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Refund amount must be greater than zero"
        )
        BigDecimal amount,

        @NotBlank(message = "Refund reason is required")
        @Size(
                max = 500,
                message = "Refund reason must not exceed 500 characters"
        )
        String reason,

        @Size(
                max = 100,
                message = "External reference must not exceed 100 characters"
        )
        String externalReference

) {
}