package com.centerflow.academic.common.exception;

public class ScheduleConflictException
        extends RuntimeException {

    public ScheduleConflictException(
            String message
    ) {
        super(message);
    }

    public ScheduleConflictException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}