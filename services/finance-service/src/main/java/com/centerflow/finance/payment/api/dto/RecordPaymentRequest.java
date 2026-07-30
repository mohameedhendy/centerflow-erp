package com.centerflow.finance.payment.api.dto;

import com.centerflow.finance.payment.domain.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RecordPaymentRequest(

        @NotNull(message = "Payment amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Payment amount must be greater than zero"
        )
        BigDecimal amount,

        @NotNull(message = "Payment method is required")
        PaymentMethod method,

        @Size(
                max = 100,
                message = "External reference must not exceed 100 characters"
        )
        String externalReference

) {
}