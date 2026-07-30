package com.centerflow.finance.expense.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidExpenseException
        extends RuntimeException {

    public InvalidExpenseException(String message) {
        super(message);
    }
}