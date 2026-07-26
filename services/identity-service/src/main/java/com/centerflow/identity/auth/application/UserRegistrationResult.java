package com.centerflow.identity.auth.application;

import com.centerflow.identity.authorization.domain.RoleName;
import com.centerflow.identity.user.domain.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserRegistrationResult(
        UUID id,
        String email,
        UserStatus status,
        boolean emailVerified,
        RoleName role,
        Instant createdAt
) {
}