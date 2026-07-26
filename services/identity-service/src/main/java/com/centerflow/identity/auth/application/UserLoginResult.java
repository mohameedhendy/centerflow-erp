package com.centerflow.identity.auth.application;

import com.centerflow.identity.authorization.domain.RoleName;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserLoginResult(
        String accessToken,
        String tokenType,
        long expiresIn,
        Instant expiresAt,
        String refreshToken,
        long refreshTokenExpiresIn,
        Instant refreshTokenExpiresAt,
        UUID userId,
        String email,
        List<RoleName> roles
) {
}