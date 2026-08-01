package com.centerflow.identity.authorization.api;

import com.centerflow.identity.authorization.application.UserRoleAssignmentResult;
import com.centerflow.identity.authorization.application.UserRoleAssignmentService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(
        "/api/v1/auth/admin/users"
)
public class AdminUserRoleController {

    private final UserRoleAssignmentService
            roleAssignmentService;

    public AdminUserRoleController(
            UserRoleAssignmentService
                    roleAssignmentService
    ) {
        this.roleAssignmentService =
                roleAssignmentService;
    }

    @PutMapping("/{userId}/roles")
    public UserRoleAssignmentResponse replaceRoles(
            @PathVariable UUID userId,
            @Valid
            @RequestBody
            ReplaceUserRolesRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID assignedBy =
                UUID.fromString(
                        jwt.getSubject()
                );

        UserRoleAssignmentResult result =
                roleAssignmentService.replaceRoles(
                        userId,
                        request.roles(),
                        assignedBy
                );

        return UserRoleAssignmentResponse.from(
                result
        );
    }
}