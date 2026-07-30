package com.centerflow.finance.refund.exception;

public class RefundConflictException
        extends RuntimeException {

    public RefundConflictException(String message) {
        super(message);
    }
}