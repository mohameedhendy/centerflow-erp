package com.centerflow.finance.account.exception;

public class EnrollmentFinancialAccountConflictException
        extends RuntimeException {

    public EnrollmentFinancialAccountConflictException(
            String message
    ) {
        super(message);
    }
}