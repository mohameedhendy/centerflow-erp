package com.centerflow.identity.common.exception;

public class CannotRemoveOwnAdminRoleException
        extends RuntimeException {

    public CannotRemoveOwnAdminRoleException() {
        super(
                "An administrator cannot remove their own ADMIN role"
        );
    }
}