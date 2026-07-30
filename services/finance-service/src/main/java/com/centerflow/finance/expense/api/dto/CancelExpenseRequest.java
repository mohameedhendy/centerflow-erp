package com.centerflow.finance.expense.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelExpenseRequest(

        @NotBlank(
                message = "Cancellation reason is required"
        )
        @Size(
                min = 3,
                max = 500,
                message = "Cancellation reason must be between "
                        + "3 and 500 characters"
        )
        String reason

) {
}