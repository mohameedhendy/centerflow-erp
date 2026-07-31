package com.centerflow.finance.report.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FinancialOverviewResponse(

        String currency,
        LocalDate fromDate,
        LocalDate toDate,

        AccountPortfolioSummary accounts,
        PeriodCashFlowSummary cashFlow,
        AdjustmentSummary adjustments,
        CurrentLiabilitySummary currentLiabilities,

        List<PaymentMethodTotal> paymentMethods,
        List<ExpenseCategoryTotal> expenseCategories

) {

    public record AccountPortfolioSummary(

            long totalAccounts,
            long openAccounts,
            long settledAccounts,
            long cancelledAccounts,

            BigDecimal billedAmount,
            BigDecimal paidAmount,
            BigDecimal outstandingAmount,

            long overdueInstallments,
            BigDecimal overdueAmount

    ) {
    }

    public record PeriodCashFlowSummary(

            long paymentCount,
            BigDecimal collectedAmount,

            long refundCount,
            BigDecimal refundedAmount,

            long expenseCount,
            BigDecimal expenseAmount,

            long instructorPaymentCount,
            BigDecimal instructorPaidAmount,

            BigDecimal netCashFlow

    ) {
    }

    public record AdjustmentSummary(

            long discountCount,
            BigDecimal discountAmount,

            long chargeCount,
            BigDecimal chargeAmount

    ) {
    }

    public record CurrentLiabilitySummary(

            long accruedInstructorEarnings,
            BigDecimal accruedInstructorAmount

    ) {
    }

    public record PaymentMethodTotal(

            String paymentMethod,
            long paymentCount,
            BigDecimal amount

    ) {
    }

    public record ExpenseCategoryTotal(

            String category,
            long expenseCount,
            BigDecimal amount

    ) {
    }
}