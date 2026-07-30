package com.centerflow.finance.account.domain;

public class InvalidFinancialAccountPaymentException
        extends IllegalStateException {

    public InvalidFinancialAccountPaymentException(
            String message
    ) {
        super(message);
    }
}