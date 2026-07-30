package com.centerflow.finance.adjustment.exception;

public class FinancialAdjustmentConflictException
        extends RuntimeException {

    public FinancialAdjustmentConflictException(
            String message
    ) {
        super(message);
    }
}