package com.centerflow.identity.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        @Size(
                max = 320,
                message = "Email must not exceed 320 characters"
        )
        String email,

        @NotBlank(message = "Password is required")
        @Size(
                min = 8,
                max = 64,
                message = "Password must be between 8 and 64 characters"
        )
        @Pattern(
                regexp = "^(?=.*\\p{L})(?=.*\\d).+$",
                message = "Password must contain at least one letter and one number"
        )
        String password
) {
}