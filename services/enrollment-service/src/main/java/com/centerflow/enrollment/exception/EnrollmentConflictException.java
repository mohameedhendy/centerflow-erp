package com.centerflow.enrollment.exception;

public class EnrollmentConflictException
        extends RuntimeException {

    public EnrollmentConflictException(
            String message
    ) {
        super(message);
    }
}