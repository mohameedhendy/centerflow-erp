package com.centerflow.academic.courselevel.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCourseLevelRequest(

        @NotBlank(message = "Course level name is required")
        @Size(
                max = 150,
                message = "Course level name must not exceed 150 characters"
        )
        String name,

        @NotNull(message = "Sequence number is required")
        @Min(
                value = 1,
                message = "Sequence number must be at least 1"
        )
        @Max(
                value = 100,
                message = "Sequence number must not exceed 100"
        )
        Integer sequenceNumber,

        @NotNull(message = "Duration hours are required")
        @Min(
                value = 1,
                message = "Duration hours must be at least 1"
        )
        @Max(
                value = 2000,
                message = "Duration hours must not exceed 2000"
        )
        Integer durationHours,

        @Size(
                max = 1000,
                message = "Course level description must not exceed 1000 characters"
        )
        String description
) {
}