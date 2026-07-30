package com.centerflow.finance.adjustment.domain;

public class InvalidFinancialAdjustmentException
        extends RuntimeException {

    public InvalidFinancialAdjustmentException(
            String message
    ) {
        super(message);
    }
}