package com.centerflow.enrollment.domain;

public class InvalidEnrollmentTransferException
        extends IllegalStateException {

    public InvalidEnrollmentTransferException(String message) {
        super(message);
    }
}