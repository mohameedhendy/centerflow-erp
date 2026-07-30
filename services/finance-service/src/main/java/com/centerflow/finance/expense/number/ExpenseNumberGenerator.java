package com.centerflow.finance.expense.number;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.time.ZoneOffset;

@Component
public class ExpenseNumberGenerator {

    private final JdbcTemplate jdbcTemplate;

    public ExpenseNumberGenerator(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String nextNumber() {
        Long sequenceValue = jdbcTemplate.queryForObject(
                "SELECT nextval('expense_number_sequence')",
                Long.class
        );

        if (sequenceValue == null) {
            throw new IllegalStateException(
                    "Expense sequence did not return a value"
            );
        }

        int currentYear = Year.now(
                ZoneOffset.UTC
        ).getValue();

        return "EXP-%d-%06d".formatted(
                currentYear,
                sequenceValue
        );
    }
}