package com.centerflow.finance.account.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InstallmentScheduleGeneratorTests {

    @Test
    void shouldDistributeAmountAndAddRemainderToLastInstallment() {
        LocalDate firstDueDate =
                LocalDate.of(2026, 8, 1);

        List<InstallmentScheduleGenerator.ScheduleItem>
                schedule =
                InstallmentScheduleGenerator.generate(
                        new BigDecimal("1000.00"),
                        3,
                        firstDueDate
                );

        assertThat(schedule).hasSize(3);

        assertThat(schedule)
                .extracting(
                        InstallmentScheduleGenerator
                                .ScheduleItem::amount
                )
                .containsExactly(
                        new BigDecimal("333.33"),
                        new BigDecimal("333.33"),
                        new BigDecimal("333.34")
                );

        assertThat(schedule)
                .extracting(
                        InstallmentScheduleGenerator
                                .ScheduleItem::dueDate
                )
                .containsExactly(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 10, 1)
                );

        BigDecimal total = schedule.stream()
                .map(
                        InstallmentScheduleGenerator
                                .ScheduleItem::amount
                )
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );

        assertThat(total)
                .isEqualByComparingTo("1000.00");
    }

    @Test
    void shouldRejectAmountTooSmallForInstallmentCount() {
        assertThatThrownBy(
                () -> InstallmentScheduleGenerator.generate(
                        new BigDecimal("0.02"),
                        3,
                        LocalDate.of(2026, 8, 1)
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Total amount is too small "
                                + "for installment count"
                );
    }
}