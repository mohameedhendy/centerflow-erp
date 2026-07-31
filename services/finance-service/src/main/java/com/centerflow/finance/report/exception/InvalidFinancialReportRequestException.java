package com.centerflow.finance.report.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidFinancialReportRequestException
        extends RuntimeException {

    public InvalidFinancialReportRequestException(
            String message
    ) {
        super(message);
    }
}