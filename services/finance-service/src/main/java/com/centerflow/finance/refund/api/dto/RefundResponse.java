package com.centerflow.finance.refund.api.dto;

import com.centerflow.finance.account.domain.EnrollmentFinancialAccount;
import com.centerflow.finance.account.domain.FinancialAccountStatus;
import com.centerflow.finance.payment.domain.Payment;
import com.centerflow.finance.payment.domain.PaymentStatus;
import com.centerflow.finance.refund.domain.Refund;
import com.centerflow.finance.refund.domain.RefundAllocation;
import com.centerflow.finance.refund.domain.RefundStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RefundResponse(

        UUID id,
        String refundNumber,
        UUID paymentId,
        String paymentNumber,
        UUID enrollmentId,
        BigDecimal amount,
        String currency,
        String reason,
        String externalReference,
        RefundStatus status,
        BigDecimal paymentRefundedAmount,
        BigDecimal paymentRefundableAmount,
        PaymentStatus paymentStatus,
        BigDecimal accountPaidAmount,
        BigDecimal accountRemainingAmount,
        boolean initialPaymentSatisfied,
        FinancialAccountStatus financialAccountStatus,
        List<RefundAllocationResponse> allocations,
        Instant recordedAt

) {

    public static RefundResponse from(
            Refund refund,
            Payment payment,
            EnrollmentFinancialAccount account,
            List<RefundAllocation> allocations
    ) {
        return new RefundResponse(
                refund.getId(),
                refund.getRefundNumber(),
                payment.getId(),
                payment.getPaymentNumber(),
                account.getEnrollmentId(),
                refund.getAmount(),
                refund.getCurrency(),
                refund.getReason(),
                refund.getExternalReference(),
                refund.getStatus(),
                payment.getRefundedAmount(),
                payment.getRefundableAmount(),
                payment.getStatus(),
                account.getPaidAmount(),
                account.getRemainingAmount(),
                account.isInitialPaymentSatisfied(),
                account.getStatus(),
                allocations.stream()
                        .map(RefundAllocationResponse::from)
                        .toList(),
                refund.getRecordedAt()
        );
    }
}