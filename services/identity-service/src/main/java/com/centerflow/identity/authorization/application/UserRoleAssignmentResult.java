package com.centerflow.identity.authorization.application;

import com.centerflow.identity.authorization.domain.RoleName;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserRoleAssignmentResult(
        UUID userId,
        String email,
        List<RoleName> roles,
        UUID assignedBy,
        Instant assignedAt
) {
}