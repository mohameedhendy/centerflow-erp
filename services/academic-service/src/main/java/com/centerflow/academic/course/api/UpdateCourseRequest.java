package com.centerflow.academic.course.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCourseRequest(

        @NotBlank(message = "Course name is required")
        @Size(
                max = 150,
                message = "Course name must not exceed 150 characters"
        )
        String name,

        @Size(
                max = 1000,
                message = "Course description must not exceed 1000 characters"
        )
        String description
) {
}