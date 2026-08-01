package com.centerflow.identity.common.exception;

import java.util.UUID;

public class IdentityUserNotFoundException
        extends RuntimeException {

    public IdentityUserNotFoundException(UUID userId) {
        super(
                "Identity user was not found: "
                        + userId
        );
    }
}