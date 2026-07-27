package com.centerflow.academic.instructor.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateInstructorRequest(

        @NotBlank(message = "Instructor code is required")
        @Size(
                max = 30,
                message = "Instructor code must not exceed 30 characters"
        )
        @Pattern(
                regexp = "^[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*$",
                message = "Instructor code may contain letters, numbers, and single hyphens only"
        )
        String code,

        @NotBlank(message = "Instructor first name is required")
        @Size(
                max = 100,
                message = "First name must not exceed 100 characters"
        )
        String firstName,

        @NotBlank(message = "Instructor last name is required")
        @Size(
                max = 100,
                message = "Last name must not exceed 100 characters"
        )
        String lastName,

        @Email(message = "Email format is invalid")
        @Size(
                max = 320,
                message = "Email must not exceed 320 characters"
        )
        String email,

        @Size(
                max = 30,
                message = "Phone must not exceed 30 characters"
        )
        String phone,

        @Size(
                max = 150,
                message = "Specialization must not exceed 150 characters"
        )
        String specialization,

        @Size(
                max = 1000,
                message = "Bio must not exceed 1000 characters"
        )
        String bio
) {
}