package com.centerflow.enrollment.number;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.time.ZoneOffset;
import java.util.Locale;

@Component
public class EnrollmentNumberGenerator {

    private static final String NEXT_SEQUENCE_VALUE_SQL =
            "SELECT nextval('enrollment_number_sequence')";

    private final JdbcTemplate jdbcTemplate;

    public EnrollmentNumberGenerator(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String nextNumber() {
        Long sequenceValue = jdbcTemplate.queryForObject(
                NEXT_SEQUENCE_VALUE_SQL,
                Long.class
        );

        if (sequenceValue == null) {
            throw new IllegalStateException(
                    "Could not generate enrollment number"
            );
        }

        int currentYear = Year.now(ZoneOffset.UTC)
                .getValue();

        return String.format(
                Locale.ROOT,
                "ENR-%d-%06d",
                currentYear,
                sequenceValue
        );
    }
}