package com.centerflow.academic.common.exception;

public class AttendanceNotAllowedException
        extends RuntimeException {

    public AttendanceNotAllowedException(
            String message
    ) {
        super(message);
    }
}