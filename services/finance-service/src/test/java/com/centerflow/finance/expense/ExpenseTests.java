package com.centerflow.finance.expense;

import com.centerflow.finance.expense.domain.Expense;
import com.centerflow.finance.expense.domain.ExpenseCategory;
import com.centerflow.finance.expense.domain.ExpenseStatus;
import com.centerflow.finance.expense.exception.InvalidExpenseException;
import com.centerflow.finance.payment.domain.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpenseTests {

    @Test
    void expenseShouldBeCreatedWithNormalizedValues() {
        Expense expense = Expense.create(
                "EXP-2026-000001",
                UUID.randomUUID(),
                ExpenseCategory.RENT,
                new BigDecimal("5000.00"),
                "egp",
                PaymentMethod.BANK_TRANSFER,
                "  Property Owner  ",
                "  Monthly branch rent  ",
                LocalDate.now(ZoneOffset.UTC),
                "  RENT-REFERENCE-1  "
        );

        assertThat(expense.getStatus())
                .isEqualTo(ExpenseStatus.RECORDED);

        assertThat(expense.getCurrency())
                .isEqualTo("EGP");

        assertThat(expense.getPayee())
                .isEqualTo("Property Owner");

        assertThat(expense.getDescription())
                .isEqualTo("Monthly branch rent");

        assertThat(expense.getExternalReference())
                .isEqualTo("RENT-REFERENCE-1");

        assertThat(expense.getCancelledAt())
                .isNull();
    }

    @Test
    void cancellationShouldBeIdempotent() {
        Expense expense = createExpense();

        expense.cancel("Incorrect expense entry");

        var firstCancelledAt = expense.getCancelledAt();

        expense.cancel("Another reason");

        assertThat(expense.getStatus())
                .isEqualTo(ExpenseStatus.CANCELLED);

        assertThat(expense.getCancellationReason())
                .isEqualTo("Incorrect expense entry");

        assertThat(expense.getCancelledAt())
                .isEqualTo(firstCancelledAt);
    }

    @Test
    void futureExpenseDateShouldBeRejected() {
        assertThatThrownBy(
                () -> Expense.create(
                        "EXP-2026-000002",
                        null,
                        ExpenseCategory.OTHER,
                        new BigDecimal("100.00"),
                        "EGP",
                        PaymentMethod.CASH,
                        "Test Payee",
                        "Future expense",
                        LocalDate.now(ZoneOffset.UTC)
                                .plusDays(1),
                        null
                )
        )
                .isInstanceOf(
                        InvalidExpenseException.class
                )
                .hasMessageContaining(
                        "cannot be in the future"
                );
    }

    private Expense createExpense() {
        return Expense.create(
                "EXP-2026-000003",
                UUID.randomUUID(),
                ExpenseCategory.SUPPLIES,
                new BigDecimal("250.00"),
                "EGP",
                PaymentMethod.CASH,
                "Office Supplier",
                "Office supplies",
                LocalDate.now(ZoneOffset.UTC),
                null
        );
    }
}