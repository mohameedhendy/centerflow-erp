package com.centerflow.finance.earning.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateInstructorEarningRequest(

        @NotNull(
                message = "Instructor ID is required"
        )
        UUID instructorId,

        @NotNull(
                message = "Session ID is required"
        )
        UUID sessionId,

        @NotNull(
                message = "Batch ID is required"
        )
        UUID batchId,

        @NotNull(
                message = "Earning amount is required"
        )
        @DecimalMin(
                value = "0.01",
                message = "Earning amount must be "
                        + "greater than zero"
        )
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Earning amount must have "
                        + "no more than two decimal places"
        )
        BigDecimal amount,

        @NotBlank(
                message = "Earning currency is required"
        )
        @Pattern(
                regexp = "(?i)[A-Z]{3}",
                message = "Earning currency must contain "
                        + "exactly three letters"
        )
        String currency,

        @NotNull(
                message = "Session date is required"
        )
        @PastOrPresent(
                message = "Session date cannot be in the future"
        )
        LocalDate sessionDate,

        @NotBlank(
                message = "Earning description is required"
        )
        @Size(
                min = 3,
                max = 500,
                message = "Earning description must be "
                        + "between 3 and 500 characters"
        )
        String description

) {
}