package com.centerflow.finance.earning.api.dto;

import com.centerflow.finance.payment.domain.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PayInstructorEarningRequest(

        @NotNull(
                message = "Payment method is required"
        )
        PaymentMethod paymentMethod,

        @NotBlank(
                message = "Payment reference is required"
        )
        @Size(
                min = 3,
                max = 100,
                message = "Payment reference must be "
                        + "between 3 and 100 characters"
        )
        String paymentReference

) {
}