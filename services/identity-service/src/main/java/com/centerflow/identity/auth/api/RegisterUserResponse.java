package com.centerflow.identity.auth.api;

import com.centerflow.identity.auth.application.UserRegistrationResult;
import com.centerflow.identity.authorization.domain.RoleName;
import com.centerflow.identity.user.domain.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record RegisterUserResponse(
        UUID id,
        String email,
        UserStatus status,
        boolean emailVerified,
        RoleName role,
        Instant createdAt
) {

    public static RegisterUserResponse from(
            UserRegistrationResult result
    ) {
        return new RegisterUserResponse(
                result.id(),
                result.email(),
                result.status(),
                result.emailVerified(),
                result.role(),
                result.createdAt()
        );
    }
}