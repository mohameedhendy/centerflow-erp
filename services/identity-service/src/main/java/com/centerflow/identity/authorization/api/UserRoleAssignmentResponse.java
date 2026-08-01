package com.centerflow.identity.authorization.api;

import com.centerflow.identity.authorization.application.UserRoleAssignmentResult;
import com.centerflow.identity.authorization.domain.RoleName;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserRoleAssignmentResponse(
        UUID userId,
        String email,
        List<RoleName> roles,
        UUID assignedBy,
        Instant assignedAt
) {

    public static UserRoleAssignmentResponse from(
            UserRoleAssignmentResult result
    ) {
        return new UserRoleAssignmentResponse(
                result.userId(),
                result.email(),
                result.roles(),
                result.assignedBy(),
                result.assignedAt()
        );
    }
}