package com.centerflow.finance.earning;

import com.centerflow.finance.earning.domain.InstructorEarning;
import com.centerflow.finance.earning.domain.InstructorEarningStatus;
import com.centerflow.finance.earning.exception.InstructorEarningConflictException;
import com.centerflow.finance.earning.exception.InvalidInstructorEarningException;
import com.centerflow.finance.payment.domain.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InstructorEarningTests {

    @Test
    void earningShouldBeCreatedAsAccrued() {
        InstructorEarning earning = createEarning();

        assertThat(earning.getStatus())
                .isEqualTo(InstructorEarningStatus.ACCRUED);

        assertThat(earning.getAmount())
                .isEqualByComparingTo("400.00");

        assertThat(earning.getCurrency())
                .isEqualTo("EGP");

        assertThat(earning.getPaidAt()).isNull();
        assertThat(earning.getCancelledAt()).isNull();
    }

    @Test
    void accruedEarningShouldBeMarkedPaid() {
        InstructorEarning earning = createEarning();

        earning.markPaid(
                PaymentMethod.BANK_TRANSFER,
                "TRANSFER-100"
        );

        assertThat(earning.getStatus())
                .isEqualTo(InstructorEarningStatus.PAID);

        assertThat(earning.getPaymentMethod())
                .isEqualTo(PaymentMethod.BANK_TRANSFER);

        assertThat(earning.getPaymentReference())
                .isEqualTo("TRANSFER-100");

        assertThat(earning.getPaidAt()).isNotNull();
    }

    @Test
    void paidEarningCannotBeCancelled() {
        InstructorEarning earning = createEarning();

        earning.markPaid(
                PaymentMethod.CASH,
                "CASH-100"
        );

        assertThatThrownBy(
                () -> earning.cancel(
                        "Incorrect earning"
                )
        )
                .isInstanceOf(
                        InstructorEarningConflictException.class
                )
                .hasMessageContaining(
                        "cannot be cancelled"
                );
    }

    @Test
    void futureSessionDateShouldBeRejected() {
        assertThatThrownBy(
                () -> InstructorEarning.create(
                        "ERN-2026-000002",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("400.00"),
                        "EGP",
                        LocalDate.now(ZoneOffset.UTC)
                                .plusDays(1),
                        "Future session earning"
                )
        )
                .isInstanceOf(
                        InvalidInstructorEarningException.class
                )
                .hasMessageContaining(
                        "cannot be in the future"
                );
    }

    private InstructorEarning createEarning() {
        return InstructorEarning.create(
                "ERN-2026-000001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("400.00"),
                "egp",
                LocalDate.now(ZoneOffset.UTC),
                "Completed mathematics session"
        );
    }
}