package com.centerflow.academic.branch.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateBranchRequest(

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