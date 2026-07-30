package com.centerflow.finance.refund.number;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.time.ZoneOffset;

@Component
public class RefundNumberGenerator {

    private final JdbcTemplate jdbcTemplate;

    public RefundNumberGenerator(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String nextNumber() {
        Long sequenceValue = jdbcTemplate.queryForObject(
                "SELECT nextval('refund_number_sequence')",
                Long.class
        );

        if (sequenceValue == null) {
            throw new IllegalStateException(
                    "Refund sequence did not return a value"
            );
        }

        int currentYear = Year.now(
                ZoneOffset.UTC
        ).getValue();

        return "REF-%d-%06d".formatted(
                currentYear,
                sequenceValue
        );
    }
}