package com.centerflow.finance.report.api;

import java.math.BigDecimal;
import java.util.UUID;

public record FinancialAccountReportResponse(

        UUID financialAccountId,
        UUID enrollmentId,
        UUID studentId,

        String pricingPlanCode,
        String status,
        String currency,

        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount,

        PaymentSummary payments,
        InstallmentSummary installments,
        AdjustmentSummary adjustments

) {

    public record PaymentSummary(

            long paymentCount,
            BigDecimal grossCollectedAmount,

            long refundCount,
            BigDecimal refundedAmount,

            BigDecimal netCollectedAmount

    ) {
    }

    public record InstallmentSummary(

            long totalInstallments,
            long pendingInstallments,
            long partiallyPaidInstallments,
            long paidInstallments,
            long overdueInstallments,
            long cancelledInstallments,

            BigDecimal scheduledAmount,
            BigDecimal paidAmount,
            BigDecimal outstandingAmount

    ) {
    }

    public record AdjustmentSummary(

            long discountCount,
            BigDecimal discountAmount,

            long chargeCount,
            BigDecimal chargeAmount

    ) {
    }
}