package com.centerflow.academic.common.exception;

public class AcademicResourceUnavailableException
        extends RuntimeException {

    public AcademicResourceUnavailableException(
            String message
    ) {
        super(message);
    }
}