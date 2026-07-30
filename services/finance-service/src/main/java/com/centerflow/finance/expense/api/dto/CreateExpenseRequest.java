package com.centerflow.finance.expense.api.dto;

import com.centerflow.finance.expense.domain.ExpenseCategory;
import com.centerflow.finance.payment.domain.PaymentMethod;
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

public record CreateExpenseRequest(

        UUID branchId,

        @NotNull(
                message = "Expense category is required"
        )
        ExpenseCategory category,

        @NotNull(
                message = "Expense amount is required"
        )
        @DecimalMin(
                value = "0.01",
                message = "Expense amount must be greater than zero"
        )
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Expense amount must have "
                        + "no more than two decimal places"
        )
        BigDecimal amount,

        @NotBlank(
                message = "Expense currency is required"
        )
        @Pattern(
                regexp = "(?i)[A-Z]{3}",
                message = "Expense currency must contain "
                        + "exactly three letters"
        )
        String currency,

        @NotNull(
                message = "Expense payment method is required"
        )
        PaymentMethod paymentMethod,

        @NotBlank(
                message = "Expense payee is required"
        )
        @Size(
                min = 2,
                max = 150,
                message = "Expense payee must be between "
                        + "2 and 150 characters"
        )
        String payee,

        @NotBlank(
                message = "Expense description is required"
        )
        @Size(
                min = 3,
                max = 500,
                message = "Expense description must be between "
                        + "3 and 500 characters"
        )
        String description,

        @NotNull(
                message = "Expense date is required"
        )
        @PastOrPresent(
                message = "Expense date cannot be in the future"
        )
        LocalDate expenseDate,

        @Size(
                max = 100,
                message = "Expense external reference must not "
                        + "exceed 100 characters"
        )
        String externalReference

) {
}