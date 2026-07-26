package com.centerflow.identity.common.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("An account already exists for email: " + email);
    }

    public DuplicateEmailException(
            String email,
            Throwable cause
    ) {
        super(
                "An account already exists for email: " + email,
                cause
        );
    }
}