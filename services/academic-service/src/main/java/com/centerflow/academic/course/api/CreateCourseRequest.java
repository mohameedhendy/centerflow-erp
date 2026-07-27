package com.centerflow.academic.course.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCourseRequest(

        @NotBlank(message = "Course code is required")
        @Size(
                max = 30,
                message = "Course code must not exceed 30 characters"
        )
        @Pattern(
                regexp = "^[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*$",
                message = "Course code may contain letters, numbers, and single hyphens only"
        )
        String code,

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