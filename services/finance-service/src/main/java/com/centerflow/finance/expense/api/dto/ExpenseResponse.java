package com.centerflow.finance.expense.api.dto;

import com.centerflow.finance.expense.domain.Expense;
import com.centerflow.finance.expense.domain.ExpenseCategory;
import com.centerflow.finance.expense.domain.ExpenseStatus;
import com.centerflow.finance.payment.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseResponse(

        UUID id,
        String expenseNumber,
        UUID branchId,
        ExpenseCategory category,
        BigDecimal amount,
        String currency,
        PaymentMethod paymentMethod,
        String payee,
        String description,
        LocalDate expenseDate,
        String externalReference,
        ExpenseStatus status,
        String cancellationReason,
        Instant createdAt,
        Instant updatedAt,
        Instant cancelledAt

) {

    public static ExpenseResponse from(
            Expense expense
    ) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getExpenseNumber(),
                expense.getBranchId(),
                expense.getCategory(),
                expense.getAmount(),
                expense.getCurrency(),
                expense.getPaymentMethod(),
                expense.getPayee(),
                expense.getDescription(),
                expense.getExpenseDate(),
                expense.getExternalReference(),
                expense.getStatus(),
                expense.getCancellationReason(),
                expense.getCreatedAt(),
                expense.getUpdatedAt(),
                expense.getCancelledAt()
        );
    }
}