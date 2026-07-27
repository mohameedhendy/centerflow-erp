package com.centerflow.academic.common.exception;

public class AttendanceConflictException
        extends RuntimeException {

    public AttendanceConflictException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}