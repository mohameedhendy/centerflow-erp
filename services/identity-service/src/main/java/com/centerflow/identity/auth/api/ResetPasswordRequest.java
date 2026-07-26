package com.centerflow.identity.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(

        @NotBlank(
                message = "Password reset token is required"
        )
        @Size(
                max = 512,
                message = "Password reset token is invalid"
        )
        String resetToken,

        @NotBlank(
                message = "New password is required"
        )
        @Size(
                min = 8,
                max = 64,
                message = "New password must be between 8 and 64 characters"
        )
        @Pattern(
                regexp = "^(?=.*\\p{L})(?=.*\\d).+$",
                message = "New password must contain at least one letter and one number"
        )
        String newPassword
) {
}