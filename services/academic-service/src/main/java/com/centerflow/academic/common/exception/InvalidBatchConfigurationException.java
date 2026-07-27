package com.centerflow.academic.common.exception;

public class InvalidBatchConfigurationException
        extends RuntimeException {

    public InvalidBatchConfigurationException(
            String message
    ) {
        super(message);
    }
}