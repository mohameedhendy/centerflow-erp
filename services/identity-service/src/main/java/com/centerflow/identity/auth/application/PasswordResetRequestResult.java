package com.centerflow.identity.auth.application;

import java.time.Instant;

public record PasswordResetRequestResult(
        boolean issued,
        String tokenValue,
        Instant expiresAt
) {

    public static PasswordResetRequestResult issued(
            String tokenValue,
            Instant expiresAt
    ) {
        return new PasswordResetRequestResult(
                true,
                tokenValue,
                expiresAt
        );
    }

    public static PasswordResetRequestResult notIssued() {
        return new PasswordResetRequestResult(
                false,
                null,
                null
        );
    }
}