package com.centerflow.identity.common.exception;

import com.centerflow.identity.authorization.domain.RoleName;

public class RequiredRoleNotFoundException
        extends RuntimeException {

    public RequiredRoleNotFoundException(RoleName roleName) {
        super(
                "Required system role is not configured: "
                        + roleName
        );
    }
}