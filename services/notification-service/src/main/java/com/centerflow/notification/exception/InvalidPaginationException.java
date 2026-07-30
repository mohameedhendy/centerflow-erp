package com.centerflow.notification.exception;

public class InvalidPaginationException
        extends RuntimeException {

    public InvalidPaginationException(
            String message
    ) {
        super(message);
    }
}