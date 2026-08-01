package com.centerflow.identity.authorization.api;

import com.centerflow.identity.authorization.domain.RoleName;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record ReplaceUserRolesRequest(

        @NotEmpty(
                message =
                        "At least one role is required"
        )
        Set<
                @NotNull(
                        message = "Role is required"
                )
                        RoleName
                > roles
) {
}