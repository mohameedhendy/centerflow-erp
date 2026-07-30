package com.centerflow.finance.payment.api.dto;

import com.centerflow.finance.account.domain.EnrollmentFinancialAccount;
import com.centerflow.finance.account.domain.FinancialAccountStatus;
import com.centerflow.finance.payment.domain.Payment;
import com.centerflow.finance.payment.domain.PaymentAllocation;
import com.centerflow.finance.payment.domain.PaymentMethod;
import com.centerflow.finance.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentResponse(

        UUID id,
        String paymentNumber,
        UUID financialAccountId,
        UUID enrollmentId,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        String externalReference,
        PaymentStatus status,
        BigDecimal accountPaidAmount,
        BigDecimal accountRemainingAmount,
        boolean initialPaymentSatisfied,
        FinancialAccountStatus financialAccountStatus,
        List<PaymentAllocationResponse> allocations,
        Instant recordedAt

) {

    public static PaymentResponse from(
            Payment payment,
            EnrollmentFinancialAccount account,
            List<PaymentAllocation> allocations
    ) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentNumber(),
                payment.getFinancialAccountId(),
                account.getEnrollmentId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMethod(),
                payment.getExternalReference(),
                payment.getStatus(),
                account.getPaidAmount(),
                account.getRemainingAmount(),
                account.isInitialPaymentSatisfied(),
                account.getStatus(),
                allocations.stream()
                        .map(PaymentAllocationResponse::from)
                        .toList(),
                payment.getRecordedAt()
        );
    }
}