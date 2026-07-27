package com.centerflow.academic.courselevel.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCourseLevelRequest(

        @NotNull(message = "Course ID is required")
        UUID courseId,

        @NotBlank(message = "Course level code is required")
        @Size(
                max = 30,
                message = "Course level code must not exceed 30 characters"
        )
        @Pattern(
                regexp = "^[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*$",
                message = "Course level code may contain letters, numbers, and single hyphens only"
        )
        String code,

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