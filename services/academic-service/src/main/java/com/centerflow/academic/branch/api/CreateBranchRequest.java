package com.centerflow.academic.branch.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateBranchRequest(

        @NotBlank(message = "Branch code is required")
        @Size(
                max = 30,
                message = "Branch code must not exceed 30 characters"
        )
        @Pattern(
                regexp = "^[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*$",
                message = "Branch code may contain letters, numbers, and single hyphens only"
        )
        String code,

        @NotBlank(message = "Branch name is required")
        @Size(
                max = 150,
                message = "Branch name must not exceed 150 characters"
        )
        String name,

        @Size(
                max = 30,
                message = "Phone must not exceed 30 characters"
        )
        String phone,

        @Email(message = "Email format is invalid")
        @Size(
                max = 320,
                message = "Email must not exceed 320 characters"
        )
        String email,

        @Size(
                max = 500,
                message = "Address must not exceed 500 characters"
        )
        String address,

        @Size(
                max = 100,
                message = "City must not exceed 100 characters"
        )
        String city
) {
}