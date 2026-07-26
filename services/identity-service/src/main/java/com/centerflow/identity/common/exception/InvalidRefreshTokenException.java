package com.centerflow.identity.common.exception;

public class InvalidRefreshTokenException
        extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token");
    }

    public InvalidRefreshTokenException(
            Throwable cause
    ) {
        super(
                "Invalid or expired refresh token",
                cause
        );
    }
}