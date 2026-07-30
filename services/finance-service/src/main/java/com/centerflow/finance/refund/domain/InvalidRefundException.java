package com.centerflow.finance.refund.domain;

public class InvalidRefundException
        extends IllegalStateException {

    public InvalidRefundException(String message) {
        super(message);
    }
}