package com.centerflow.identity.auth.api;

import com.centerflow.identity.auth.application.UserLoginResult;
import com.centerflow.identity.authorization.domain.RoleName;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LoginUserResponse(
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

    public static LoginUserResponse from(
            UserLoginResult result
    ) {
        return new LoginUserResponse(
                result.accessToken(),
                result.tokenType(),
                result.expiresIn(),
                result.expiresAt(),
                result.refreshToken(),
                result.refreshTokenExpiresIn(),
                result.refreshTokenExpiresAt(),
                result.userId(),
                result.email(),
                result.roles()
        );
    }
}