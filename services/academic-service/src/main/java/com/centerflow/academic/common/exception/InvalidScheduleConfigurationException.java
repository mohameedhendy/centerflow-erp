package com.centerflow.academic.common.exception;

public class InvalidScheduleConfigurationException
        extends RuntimeException {

    public InvalidScheduleConfigurationException(
            String message
    ) {
        super(message);
    }
}