package com.centerflow.finance.expense.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ExpenseConflictException
        extends RuntimeException {

    public ExpenseConflictException(String message) {
        super(message);
    }
}