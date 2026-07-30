package com.centerflow.finance.account.api.dto;

import com.centerflow.finance.account.domain.Installment;
import com.centerflow.finance.account.domain.InstallmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InstallmentResponse(

        UUID id,
        int installmentNumber,
        LocalDate dueDate,
        BigDecimal amount,
        BigDecimal paidAmount,
        InstallmentStatus status

) {

    public static InstallmentResponse from(
            Installment installment
    ) {
        return new InstallmentResponse(
                installment.getId(),
                installment.getInstallmentNumber(),
                installment.getDueDate(),
                installment.getAmount(),
                installment.getPaidAmount(),
                installment.getStatus()
        );
    }
}