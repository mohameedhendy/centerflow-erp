package com.centerflow.finance.common.exception;

public class InvalidPaginationException
        extends RuntimeException {

    public InvalidPaginationException(String message) {
        super(message);
    }
}