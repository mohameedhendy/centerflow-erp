package com.centerflow.academic.classroom.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateClassroomRequest(

        @NotNull(message = "Branch ID is required")
        UUID branchId,

        @NotBlank(message = "Classroom code is required")
        @Size(
                max = 30,
                message = "Classroom code must not exceed 30 characters"
        )
        @Pattern(
                regexp = "^[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*$",
                message = "Classroom code may contain letters, numbers, and single hyphens only"
        )
        String code,

        @NotBlank(message = "Classroom name is required")
        @Size(
                max = 150,
                message = "Classroom name must not exceed 150 characters"
        )
        String name,

        @NotNull(message = "Classroom capacity is required")
        @Min(
                value = 1,
                message = "Classroom capacity must be at least 1"
        )
        @Max(
                value = 1000,
                message = "Classroom capacity must not exceed 1000"
        )
        Integer capacity,

        @Size(
                max = 50,
                message = "Floor must not exceed 50 characters"
        )
        String floor
) {
}