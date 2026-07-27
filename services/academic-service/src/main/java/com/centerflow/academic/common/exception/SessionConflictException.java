package com.centerflow.academic.common.exception;

public class SessionConflictException
        extends RuntimeException {

    public SessionConflictException(
            String message
    ) {
        super(message);
    }

    public SessionConflictException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}