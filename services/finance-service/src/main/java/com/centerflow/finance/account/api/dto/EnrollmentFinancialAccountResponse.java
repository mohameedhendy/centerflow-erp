package com.centerflow.finance.account.api.dto;

import com.centerflow.finance.account.domain.EnrollmentFinancialAccount;
import com.centerflow.finance.account.domain.FinancialAccountStatus;
import com.centerflow.finance.account.domain.Installment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EnrollmentFinancialAccountResponse(

        UUID id,
        UUID enrollmentId,
        UUID studentId,
        UUID pricingPlanId,
        String pricingPlanCode,
        BigDecimal totalAmount,
        String currency,
        int installmentCount,
        BigDecimal initialPaymentAmount,
        FinancialAccountStatus status,
        List<InstallmentResponse> installments,
        Instant createdAt,
        Instant updatedAt

) {

    public static EnrollmentFinancialAccountResponse from(
            EnrollmentFinancialAccount account,
            List<Installment> installments
    ) {
        List<InstallmentResponse> installmentResponses =
                installments.stream()
                        .map(InstallmentResponse::from)
                        .toList();

        return new EnrollmentFinancialAccountResponse(
                account.getId(),
                account.getEnrollmentId(),
                account.getStudentId(),
                account.getPricingPlanId(),
                account.getPricingPlanCode(),
                account.getTotalAmount(),
                account.getCurrency(),
                account.getInstallmentCount(),
                account.getInitialPaymentAmount(),
                account.getStatus(),
                installmentResponses,
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}