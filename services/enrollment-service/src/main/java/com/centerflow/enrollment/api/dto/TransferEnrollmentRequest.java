package com.centerflow.enrollment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record TransferEnrollmentRequest(

        @NotNull(message = "Target batch ID is required")
        UUID targetBatchId,

        @NotBlank(message = "Transfer reason is required")
        @Size(
                max = 500,
                message = "Transfer reason must not exceed 500 characters"
        )
        String reason

) {
}