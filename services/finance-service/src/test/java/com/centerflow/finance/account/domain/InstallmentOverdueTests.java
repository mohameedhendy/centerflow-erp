package com.centerflow.finance.account.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InstallmentOverdueTests {

    @Test
    void pastPendingInstallmentShouldBecomeOverdue() {
        Installment installment = createInstallment(
                LocalDate.of(2026, 7, 1)
        );

        boolean changed = installment.markOverdue(
                LocalDate.of(2026, 8, 1)
        );

        assertThat(changed).isTrue();
        assertThat(installment.getStatus())
                .isEqualTo(InstallmentStatus.OVERDUE);
    }

    @Test
    void installmentDueOnAccountingDateShouldNotBeOverdue() {
        Installment installment = createInstallment(
                LocalDate.of(2026, 8, 1)
        );

        boolean changed = installment.markOverdue(
                LocalDate.of(2026, 8, 1)
        );

        assertThat(changed).isFalse();
        assertThat(installment.getStatus())
                .isEqualTo(InstallmentStatus.PENDING);
    }

    @Test
    void partialPaymentShouldPreserveOverdueStatus() {
        Installment installment = createInstallment(
                LocalDate.of(2026, 7, 1)
        );

        installment.markOverdue(
                LocalDate.of(2026, 8, 1)
        );

        installment.allocatePayment(
                new BigDecimal("100.00")
        );

        assertThat(installment.getPaidAmount())
                .isEqualByComparingTo("100.00");

        assertThat(installment.getStatus())
                .isEqualTo(InstallmentStatus.OVERDUE);
    }

    @Test
    void fullPaymentShouldCloseOverdueInstallment() {
        Installment installment = createInstallment(
                LocalDate.of(2026, 7, 1)
        );

        installment.markOverdue(
                LocalDate.of(2026, 8, 1)
        );

        installment.allocatePayment(
                new BigDecimal("300.00")
        );

        assertThat(installment.getStatus())
                .isEqualTo(InstallmentStatus.PAID);
    }

    @Test
    void refundShouldRestoreOverdueStatus() {
        Installment installment = createInstallment(
                LocalDate.of(2026, 7, 1)
        );

        installment.allocatePayment(
                new BigDecimal("300.00")
        );

        installment.refundPayment(
                new BigDecimal("50.00"),
                LocalDate.of(2026, 8, 1)
        );

        assertThat(installment.getPaidAmount())
                .isEqualByComparingTo("250.00");

        assertThat(installment.getStatus())
                .isEqualTo(InstallmentStatus.OVERDUE);
    }

    private Installment createInstallment(
            LocalDate dueDate
    ) {
        return Installment.create(
                UUID.randomUUID(),
                1,
                dueDate,
                new BigDecimal("300.00")
        );
    }
}