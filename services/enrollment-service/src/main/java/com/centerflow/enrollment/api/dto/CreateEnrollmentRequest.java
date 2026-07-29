package com.centerflow.enrollment.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateEnrollmentRequest(

        @NotNull(message = "Student ID is required")
        UUID studentId,

        @NotNull(message = "Batch ID is required")
        UUID batchId

) {
}