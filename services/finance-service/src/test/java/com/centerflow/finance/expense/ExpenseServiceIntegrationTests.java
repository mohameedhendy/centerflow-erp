package com.centerflow.finance.expense;

import com.centerflow.finance.expense.api.dto.CancelExpenseRequest;
import com.centerflow.finance.expense.api.dto.CreateExpenseRequest;
import com.centerflow.finance.expense.api.dto.ExpenseResponse;
import com.centerflow.finance.expense.application.ExpenseService;
import com.centerflow.finance.expense.domain.ExpenseCategory;
import com.centerflow.finance.expense.domain.ExpenseStatus;
import com.centerflow.finance.expense.exception.ExpenseConflictException;
import com.centerflow.finance.expense.repository.ExpenseRepository;
import com.centerflow.finance.payment.domain.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExpenseServiceIntegrationTests {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Test
    void expenseShouldBeCreatedSearchedAndCancelled() {
        UUID branchId = UUID.randomUUID();

        ExpenseResponse createdExpense =
                expenseService.createExpense(
                        createRequest(
                                branchId,
                                ExpenseCategory.RENT,
                                "Property Owner",
                                "EXP-" + UUID.randomUUID()
                        )
                );

        assertThat(createdExpense.expenseNumber())
                .startsWith("EXP-");

        assertThat(createdExpense.status())
                .isEqualTo(ExpenseStatus.RECORDED);

        var searchResponse =
                expenseService.searchExpenses(
                        branchId,
                        ExpenseCategory.RENT,
                        ExpenseStatus.RECORDED,
                        PaymentMethod.BANK_TRANSFER,
                        LocalDate.now(ZoneOffset.UTC)
                                .minusDays(1),
                        LocalDate.now(ZoneOffset.UTC),
                        "property",
                        0,
                        10
                );

        assertThat(searchResponse.totalElements())
                .isEqualTo(1);

        ExpenseResponse cancelledExpense =
                expenseService.cancelExpense(
                        createdExpense.id(),
                        new CancelExpenseRequest(
                                "Expense entered by mistake"
                        )
                );

        assertThat(cancelledExpense.status())
                .isEqualTo(ExpenseStatus.CANCELLED);

        assertThat(
                cancelledExpense.cancellationReason()
        ).isEqualTo("Expense entered by mistake");

        assertThat(cancelledExpense.cancelledAt())
                .isNotNull();
    }

    @Test
    void duplicateExternalReferenceShouldBeRejected() {
        String externalReference =
                "EXP-" + UUID.randomUUID();

        CreateExpenseRequest request =
                createRequest(
                        UUID.randomUUID(),
                        ExpenseCategory.MAINTENANCE,
                        "Maintenance Company",
                        externalReference
                );

        expenseService.createExpense(request);

        assertThatThrownBy(
                () -> expenseService.createExpense(request)
        )
                .isInstanceOf(
                        ExpenseConflictException.class
                )
                .hasMessageContaining(
                        externalReference
                );

        assertThat(expenseRepository.count())
                .isEqualTo(1);
    }

    @Test
    void searchShouldFilterExpensesInDatabase() {
        UUID firstBranchId = UUID.randomUUID();
        UUID secondBranchId = UUID.randomUUID();

        expenseService.createExpense(
                createRequest(
                        firstBranchId,
                        ExpenseCategory.UTILITIES,
                        "Electricity Company",
                        "EXP-" + UUID.randomUUID()
                )
        );

        expenseService.createExpense(
                createRequest(
                        secondBranchId,
                        ExpenseCategory.MARKETING,
                        "Marketing Agency",
                        "EXP-" + UUID.randomUUID()
                )
        );

        var result = expenseService.searchExpenses(
                firstBranchId,
                ExpenseCategory.UTILITIES,
                ExpenseStatus.RECORDED,
                null,
                null,
                null,
                null,
                0,
                10
        );

        assertThat(result.totalElements())
                .isEqualTo(1);

        assertThat(result.content())
                .extracting(ExpenseResponse::branchId)
                .containsExactly(firstBranchId);
    }

    private CreateExpenseRequest createRequest(
            UUID branchId,
            ExpenseCategory category,
            String payee,
            String externalReference
    ) {
        return new CreateExpenseRequest(
                branchId,
                category,
                new BigDecimal("1500.00"),
                "EGP",
                PaymentMethod.BANK_TRANSFER,
                payee,
                "Expense service integration test",
                LocalDate.now(ZoneOffset.UTC),
                externalReference
        );
    }
}