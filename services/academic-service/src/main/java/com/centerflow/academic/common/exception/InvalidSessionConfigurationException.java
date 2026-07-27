package com.centerflow.academic.common.exception;

public class InvalidSessionConfigurationException
        extends RuntimeException {

    public InvalidSessionConfigurationException(
            String message
    ) {
        super(message);
    }
}