package com.centerflow.finance.account.exception;

import java.util.UUID;

public class EnrollmentFinancialAccountNotFoundException
        extends RuntimeException {

    public EnrollmentFinancialAccountNotFoundException(
            UUID enrollmentId
    ) {
        super(
                "Financial account not found for enrollment: "
                        + enrollmentId
        );
    }
}