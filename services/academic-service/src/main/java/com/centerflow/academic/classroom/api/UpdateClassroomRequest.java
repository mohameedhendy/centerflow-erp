package com.centerflow.academic.classroom.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateClassroomRequest(

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