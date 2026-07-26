package com.centerflow.identity.common.exception;

public class InvalidPasswordResetTokenException
        extends RuntimeException {

    public InvalidPasswordResetTokenException() {
        super(
                "Invalid or expired password reset token"
        );
    }
}