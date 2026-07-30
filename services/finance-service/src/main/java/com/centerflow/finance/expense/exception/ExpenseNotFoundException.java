package com.centerflow.finance.expense.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ExpenseNotFoundException
        extends RuntimeException {

    public ExpenseNotFoundException(UUID expenseId) {
        super("Expense not found: " + expenseId);
    }
}