package com.centerflow.finance.account.exception;

public class InvalidInstallmentSearchException
        extends RuntimeException {

    public InvalidInstallmentSearchException(
            String message
    ) {
        super(message);
    }
}