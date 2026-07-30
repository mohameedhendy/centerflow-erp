package com.centerflow.finance.account.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class InstallmentScheduleGenerator {

    private InstallmentScheduleGenerator() {
    }

    public static List<ScheduleItem> generate(
            BigDecimal totalAmount,
            int installmentCount,
            LocalDate firstDueDate
    ) {
        Objects.requireNonNull(totalAmount);
        Objects.requireNonNull(firstDueDate);

        if (installmentCount < 1) {
            throw new IllegalArgumentException(
                    "Installment count must be one or greater"
            );
        }

        BigDecimal normalizedTotal = totalAmount.setScale(
                2,
                RoundingMode.UNNECESSARY
        );

        long totalCents = normalizedTotal
                .movePointRight(2)
                .longValueExact();

        if (totalCents < installmentCount) {
            throw new IllegalArgumentException(
                    "Total amount is too small for installment count"
            );
        }

        long baseCents = totalCents / installmentCount;
        long remainderCents =
                totalCents % installmentCount;

        List<ScheduleItem> schedule =
                new ArrayList<>(installmentCount);

        for (
                int index = 0;
                index < installmentCount;
                index++
        ) {
            long installmentCents = baseCents;

            if (index == installmentCount - 1) {
                installmentCents += remainderCents;
            }

            BigDecimal amount = BigDecimal
                    .valueOf(installmentCents, 2);

            schedule.add(
                    new ScheduleItem(
                            index + 1,
                            firstDueDate.plusMonths(index),
                            amount
                    )
            );
        }

        return List.copyOf(schedule);
    }

    public record ScheduleItem(

            int installmentNumber,
            LocalDate dueDate,
            BigDecimal amount

    ) {
    }
}