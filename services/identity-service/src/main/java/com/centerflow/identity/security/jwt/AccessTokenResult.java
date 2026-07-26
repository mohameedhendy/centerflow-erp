package com.centerflow.identity.security.jwt;

import java.time.Instant;

public record AccessTokenResult(
        String tokenValue,
        Instant issuedAt,
        Instant expiresAt
) {

    public long expiresInSeconds() {
        return expiresAt.getEpochSecond()
                - issuedAt.getEpochSecond();
    }
}