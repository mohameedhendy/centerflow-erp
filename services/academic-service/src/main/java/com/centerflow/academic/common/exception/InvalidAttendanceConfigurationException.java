package com.centerflow.academic.common.exception;

public class InvalidAttendanceConfigurationException
        extends RuntimeException {

    public InvalidAttendanceConfigurationException(
            String message
    ) {
        super(message);
    }
}