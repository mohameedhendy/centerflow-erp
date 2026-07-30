package com.centerflow.finance.earning.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelInstructorEarningRequest(

        @NotBlank(
                message = "Cancellation reason is required"
        )
        @Size(
                min = 3,
                max = 500,
                message = "Cancellation reason must be "
                        + "between 3 and 500 characters"
        )
        String reason

) {
}