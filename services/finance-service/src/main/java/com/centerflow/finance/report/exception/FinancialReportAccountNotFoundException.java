package com.centerflow.finance.report.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class FinancialReportAccountNotFoundException
        extends RuntimeException {

    public FinancialReportAccountNotFoundException(
            UUID financialAccountId
    ) {
        super(
                "Financial account was not found: "
                        + financialAccountId
        );
    }
}