package com.centerflow.finance.account.api.dto;

import com.centerflow.finance.account.domain.EnrollmentFinancialAccount;
import com.centerflow.finance.account.domain.Installment;
import com.centerflow.finance.account.domain.InstallmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InstallmentCollectionResponse(

        UUID id,
        UUID financialAccountId,
        UUID enrollmentId,
        UUID studentId,
        int installmentNumber,
        LocalDate dueDate,
        BigDecimal amount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        String currency,
        InstallmentStatus status

) {

    public static InstallmentCollectionResponse from(
            Installment installment,
            EnrollmentFinancialAccount account
    ) {
        return new InstallmentCollectionResponse(
                installment.getId(),
                installment.getFinancialAccountId(),
                account.getEnrollmentId(),
                account.getStudentId(),
                installment.getInstallmentNumber(),
                installment.getDueDate(),
                installment.getAmount(),
                installment.getPaidAmount(),
                installment.getRemainingAmount(),
                account.getCurrency(),
                installment.getStatus()
        );
    }
}