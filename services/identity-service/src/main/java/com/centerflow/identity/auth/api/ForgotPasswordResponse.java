package com.centerflow.identity.auth.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ForgotPasswordResponse(
        String message,
        String resetToken,
        Instant resetTokenExpiresAt
) {
}