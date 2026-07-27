package com.centerflow.academic.batch.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateBatchRequest(

        @NotBlank(message = "Batch name is required")
        @Size(
                max = 150,
                message = "Batch name must not exceed 150 characters"
        )
        String name,

        @NotNull(message = "Branch ID is required")
        UUID branchId,

        @NotNull(message = "Classroom ID is required")
        UUID classroomId,

        @NotNull(message = "Course level ID is required")
        UUID courseLevelId,

        @NotNull(message = "Instructor ID is required")
        UUID instructorId,

        @NotNull(message = "Batch capacity is required")
        @Min(
                value = 1,
                message = "Batch capacity must be at least 1"
        )
        @Max(
                value = 1000,
                message = "Batch capacity must not exceed 1000"
        )
        Integer capacity,

        @NotNull(message = "Batch start date is required")
        LocalDate startDate,

        @NotNull(message = "Batch end date is required")
        LocalDate endDate
) {
}