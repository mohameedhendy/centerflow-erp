package com.centerflow.finance.report.repository;

import com.centerflow.finance.report.api.FinancialAccountReportResponse;
import com.centerflow.finance.report.api.FinancialOverviewResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.centerflow.finance.report.api.FinancialAccountReportResponse.InstallmentSummary;
import static com.centerflow.finance.report.api.FinancialAccountReportResponse.PaymentSummary;
import static com.centerflow.finance.report.api.FinancialOverviewResponse.AccountPortfolioSummary;
import static com.centerflow.finance.report.api.FinancialOverviewResponse.CurrentLiabilitySummary;
import static com.centerflow.finance.report.api.FinancialOverviewResponse.ExpenseCategoryTotal;
import static com.centerflow.finance.report.api.FinancialOverviewResponse.PaymentMethodTotal;
import static com.centerflow.finance.report.api.FinancialOverviewResponse.PeriodCashFlowSummary;

@Repository
public class FinancialReportQueryRepository {

    private static final BigDecimal ZERO =
            new BigDecimal("0.00");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public FinancialReportQueryRepository(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public FinancialOverviewResponse findOverview(
            String currency,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        MapSqlParameterSource parameters =
                createPeriodParameters(
                        currency,
                        fromDate,
                        toDate
                );

        AccountPortfolioSummary accounts =
                findAccountPortfolio(parameters);

        PeriodCashFlowSummary cashFlow =
                findPeriodCashFlow(parameters);

        FinancialOverviewResponse.AdjustmentSummary
                adjustments =
                findPeriodAdjustments(parameters);

        CurrentLiabilitySummary currentLiabilities =
                findCurrentLiabilities(parameters);

        List<PaymentMethodTotal> paymentMethods =
                findPaymentMethodTotals(parameters);

        List<ExpenseCategoryTotal> expenseCategories =
                findExpenseCategoryTotals(parameters);

        return new FinancialOverviewResponse(
                currency,
                fromDate,
                toDate,
                accounts,
                cashFlow,
                adjustments,
                currentLiabilities,
                paymentMethods,
                expenseCategories
        );
    }

    public Optional<FinancialAccountReportResponse>
    findAccountReport(
            UUID financialAccountId
    ) {
        MapSqlParameterSource parameters =
                new MapSqlParameterSource()
                        .addValue(
                                "financialAccountId",
                                financialAccountId
                        );

        List<AccountDetails> accounts =
                jdbcTemplate.query(
                        """
                        SELECT
                            account.id,
                            account.enrollment_id,
                            account.student_id,
                            account.pricing_plan_code,
                            account.status,
                            account.currency,
                            account.total_amount,
                            account.paid_amount
                        FROM enrollment_financial_accounts account
                        WHERE account.id = :financialAccountId
                        """,
                        parameters,
                        (resultSet, rowNumber) ->
                                new AccountDetails(
                                        readUuid(
                                                resultSet,
                                                "id"
                                        ),
                                        readUuid(
                                                resultSet,
                                                "enrollment_id"
                                        ),
                                        readUuid(
                                                resultSet,
                                                "student_id"
                                        ),
                                        resultSet.getString(
                                                "pricing_plan_code"
                                        ),
                                        resultSet.getString(
                                                "status"
                                        ),
                                        resultSet.getString(
                                                "currency"
                                        ),
                                        money(
                                                resultSet,
                                                "total_amount"
                                        ),
                                        money(
                                                resultSet,
                                                "paid_amount"
                                        )
                                )
                );

        if (accounts.isEmpty()) {
            return Optional.empty();
        }

        AccountDetails account = accounts.getFirst();

        PaymentSummary payments =
                findAccountPaymentSummary(parameters);

        InstallmentSummary installments =
                findAccountInstallmentSummary(parameters);

        FinancialAccountReportResponse.AdjustmentSummary
                adjustments =
                findAccountAdjustments(parameters);

        BigDecimal outstandingAmount =
                positiveDifference(
                        account.totalAmount(),
                        account.paidAmount()
                );

        return Optional.of(
                new FinancialAccountReportResponse(
                        account.financialAccountId(),
                        account.enrollmentId(),
                        account.studentId(),
                        account.pricingPlanCode(),
                        account.status(),
                        account.currency(),
                        account.totalAmount(),
                        account.paidAmount(),
                        outstandingAmount,
                        payments,
                        installments,
                        adjustments
                )
        );
    }

    private AccountPortfolioSummary findAccountPortfolio(
            MapSqlParameterSource parameters
    ) {
        AccountPortfolioBase base =
                jdbcTemplate.queryForObject(
                        """
                        SELECT
                            COUNT(*) AS total_accounts,

                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN status = 'OPEN'
                                        THEN 1
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS open_accounts,

                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN status = 'SETTLED'
                                        THEN 1
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS settled_accounts,

                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN status = 'CANCELLED'
                                        THEN 1
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS cancelled_accounts,

                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN status <> 'CANCELLED'
                                        THEN total_amount
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS billed_amount,

                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN status <> 'CANCELLED'
                                        THEN paid_amount
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS paid_amount

                        FROM enrollment_financial_accounts
                        WHERE currency = :currency
                        """,
                        parameters,
                        (resultSet, rowNumber) ->
                                new AccountPortfolioBase(
                                        resultSet.getLong(
                                                "total_accounts"
                                        ),
                                        resultSet.getLong(
                                                "open_accounts"
                                        ),
                                        resultSet.getLong(
                                                "settled_accounts"
                                        ),
                                        resultSet.getLong(
                                                "cancelled_accounts"
                                        ),
                                        money(
                                                resultSet,
                                                "billed_amount"
                                        ),
                                        money(
                                                resultSet,
                                                "paid_amount"
                                        )
                                )
                );

        CountAmount overdue =
                queryCountAmount(
                        """
                        SELECT
                            COUNT(*) AS item_count,

                            COALESCE(
                                SUM(
                                    installment.amount
                                    - installment.paid_amount
                                ),
                                0
                            ) AS total_amount

                        FROM installments installment
                        JOIN enrollment_financial_accounts account
                          ON account.id =
                             installment.financial_account_id

                        WHERE account.currency = :currency
                          AND account.status <> 'CANCELLED'
                          AND installment.status = 'OVERDUE'
                        """,
                        parameters
                );

        if (base == null) {
            base = AccountPortfolioBase.empty();
        }

        BigDecimal outstandingAmount =
                positiveDifference(
                        base.billedAmount(),
                        base.paidAmount()
                );

        return new AccountPortfolioSummary(
                base.totalAccounts(),
                base.openAccounts(),
                base.settledAccounts(),
                base.cancelledAccounts(),
                base.billedAmount(),
                base.paidAmount(),
                outstandingAmount,
                overdue.count(),
                overdue.amount()
        );
    }

    private PeriodCashFlowSummary findPeriodCashFlow(
            MapSqlParameterSource parameters
    ) {
        CountAmount payments =
                queryCountAmount(
                        """
                        SELECT
                            COUNT(*) AS item_count,
                            COALESCE(
                                SUM(amount),
                                0
                            ) AS total_amount
                        FROM payments
                        WHERE currency = :currency
                          AND recorded_at >= :fromInstant
                          AND recorded_at < :toExclusiveInstant
                        """,
                        parameters
                );

        CountAmount refunds =
                queryCountAmount(
                        """
                        SELECT
                            COUNT(*) AS item_count,
                            COALESCE(
                                SUM(amount),
                                0
                            ) AS total_amount
                        FROM refunds
                        WHERE currency = :currency
                          AND recorded_at >= :fromInstant
                          AND recorded_at < :toExclusiveInstant
                        """,
                        parameters
                );

        CountAmount expenses =
                queryCountAmount(
                        """
                        SELECT
                            COUNT(*) AS item_count,
                            COALESCE(
                                SUM(amount),
                                0
                            ) AS total_amount
                        FROM expenses
                        WHERE currency = :currency
                          AND status = 'RECORDED'
                          AND expense_date >= :fromDate
                          AND expense_date <= :toDate
                        """,
                        parameters
                );

        CountAmount instructorPayments =
                queryCountAmount(
                        """
                        SELECT
                            COUNT(*) AS item_count,
                            COALESCE(
                                SUM(amount),
                                0
                            ) AS total_amount
                        FROM instructor_earnings
                        WHERE currency = :currency
                          AND status = 'PAID'
                          AND paid_at >= :fromInstant
                          AND paid_at < :toExclusiveInstant
                        """,
                        parameters
                );

        BigDecimal netCashFlow =
                payments.amount()
                        .subtract(refunds.amount())
                        .subtract(expenses.amount())
                        .subtract(
                                instructorPayments.amount()
                        );

        return new PeriodCashFlowSummary(
                payments.count(),
                payments.amount(),
                refunds.count(),
                refunds.amount(),
                expenses.count(),
                expenses.amount(),
                instructorPayments.count(),
                instructorPayments.amount(),
                netCashFlow
        );
    }

    private FinancialOverviewResponse.AdjustmentSummary
    findPeriodAdjustments(
            MapSqlParameterSource parameters
    ) {
        FinancialOverviewResponse.AdjustmentSummary result =
                jdbcTemplate.queryForObject(
                        """
                        SELECT
                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN type = 'DISCOUNT'
                                        THEN 1
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS discount_count,

                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN type = 'DISCOUNT'
                                        THEN amount
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS discount_amount,

                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN type = 'CHARGE'
                                        THEN 1
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS charge_count,

                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN type = 'CHARGE'
                                        THEN amount
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS charge_amount

                        FROM financial_adjustments
                        WHERE currency = :currency
                          AND created_at >= :fromInstant
                          AND created_at < :toExclusiveInstant
                        """,
                        parameters,
                        (resultSet, rowNumber) ->
                                new FinancialOverviewResponse
                                        .AdjustmentSummary(
                                        resultSet.getLong(
                                                "discount_count"
                                        ),
                                        money(
                                                resultSet,
                                                "discount_amount"
                                        ),
                                        resultSet.getLong(
                                                "charge_count"
                                        ),
                                        money(
                                                resultSet,
                                                "charge_amount"
                                        )
                                )
                );

        if (result == null) {
            return new FinancialOverviewResponse
                    .AdjustmentSummary(
                    0,
                    ZERO,
                    0,
                    ZERO
            );
        }

        return result;
    }

    private CurrentLiabilitySummary
    findCurrentLiabilities(
            MapSqlParameterSource parameters
    ) {
        CountAmount accruedEarnings =
                queryCountAmount(
                        """
                        SELECT
                            COUNT(*) AS item_count,
                            COALESCE(
                                SUM(amount),
                                0
                            ) AS total_amount
                        FROM instructor_earnings
                        WHERE currency = :currency
                          AND status = 'ACCRUED'
                        """,
                        parameters
                );

        return new CurrentLiabilitySummary(
                accruedEarnings.count(),
                accruedEarnings.amount()
        );
    }

    private List<PaymentMethodTotal>
    findPaymentMethodTotals(
            MapSqlParameterSource parameters
    ) {
        return jdbcTemplate.query(
                """
                SELECT
                    method,
                    COUNT(*) AS payment_count,
                    COALESCE(
                        SUM(amount),
                        0
                    ) AS total_amount
                FROM payments
                WHERE currency = :currency
                  AND recorded_at >= :fromInstant
                  AND recorded_at < :toExclusiveInstant
                GROUP BY method
                ORDER BY method
                """,
                parameters,
                (resultSet, rowNumber) ->
                        new PaymentMethodTotal(
                                resultSet.getString(
                                        "method"
                                ),
                                resultSet.getLong(
                                        "payment_count"
                                ),
                                money(
                                        resultSet,
                                        "total_amount"
                                )
                        )
        );
    }

    private List<ExpenseCategoryTotal>
    findExpenseCategoryTotals(
            MapSqlParameterSource parameters
    ) {
        return jdbcTemplate.query(
                """
                SELECT
                    category,
                    COUNT(*) AS expense_count,
                    COALESCE(
                        SUM(amount),
                        0
                    ) AS total_amount
                FROM expenses
                WHERE currency = :currency
                  AND status = 'RECORDED'
                  AND expense_date >= :fromDate
                  AND expense_date <= :toDate
                GROUP BY category
                ORDER BY category
                """,
                parameters,
                (resultSet, rowNumber) ->
                        new ExpenseCategoryTotal(
                                resultSet.getString(
                                        "category"
                                ),
                                resultSet.getLong(
                                        "expense_count"
                                ),
                                money(
                                        resultSet,
                                        "total_amount"
                                )
                        )
        );
    }

    private PaymentSummary findAccountPaymentSummary(
            MapSqlParameterSource parameters
    ) {
        PaymentSummary result =
                jdbcTemplate.queryForObject(
                        """
                        SELECT
                            (
                                SELECT COUNT(*)
                                FROM payments payment
                                WHERE payment.financial_account_id =
                                      :financialAccountId
                            ) AS payment_count,

                            (
                                SELECT COALESCE(
                                    SUM(payment.amount),
                                    0
                                )
                                FROM payments payment
                                WHERE payment.financial_account_id =
                                      :financialAccountId
                            ) AS gross_collected_amount,

                            (
                                SELECT COUNT(*)
                                FROM refunds refund
                                JOIN payments payment
                                  ON payment.id =
                                     refund.payment_id
                                WHERE payment.financial_account_id =
                                      :financialAccountId
                            ) AS refund_count,

                            (
                                SELECT COALESCE(
                                    SUM(refund.amount),
                                    0
                                )
                                FROM refunds refund
                                JOIN payments payment
                                  ON payment.id =
                                     refund.payment_id
                                WHERE payment.financial_account_id =
                                      :financialAccountId
                            ) AS refunded_amount
                        """,
                        parameters,
                        (resultSet, rowNumber) -> {
                            BigDecimal grossCollected =
                                    money(
                                            resultSet,
                                            "gross_collected_amount"
                                    );

                            BigDecimal refunded =
                                    money(
                                            resultSet,
                                            "refunded_amount"
                                    );

                            return new PaymentSummary(
                                    resultSet.getLong(
                                            "payment_count"
                                    ),
                                    grossCollected,
                                    resultSet.getLong(
                                            "refund_count"
                                    ),
                                    refunded,
                                    grossCollected.subtract(
                                            refunded
                                    )
                            );
                        }
                );

        if (result == null) {
            return new PaymentSummary(
                    0,
                    ZERO,
                    0,
                    ZERO,
                    ZERO
            );
        }

        return result;
    }

    private InstallmentSummary
    findAccountInstallmentSummary(
            MapSqlParameterSource parameters
    ) {
        InstallmentSummary result =
                jdbcTemplate.queryForObject(
                        """
                        SELECT
                            COUNT(*) AS total_installments,

                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN status = 'PENDING'
                                        THEN 1
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS pending_installments,

                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN status =
                                             'PARTIALLY_PAID'
                                        THEN 1
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS partially_paid_installments,

                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN status = 'PAID'
                                        THEN 1
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS paid_installments,

                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN status = 'OVERDUE'
                                        THEN 1
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS overdue_installments,

                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN status = 'CANCELLED'
                                        THEN 1
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS cancelled_installments,

                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN status <> 'CANCELLED'
                                        THEN amount
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS scheduled_amount,

                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN status <> 'CANCELLED'
                                        THEN paid_amount
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS paid_amount,

                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN status <> 'CANCELLED'
                                        THEN amount - paid_amount
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS outstanding_amount

                        FROM installments
                        WHERE financial_account_id =
                              :financialAccountId
                        """,
                        parameters,
                        (resultSet, rowNumber) ->
                                new InstallmentSummary(
                                        resultSet.getLong(
                                                "total_installments"
                                        ),
                                        resultSet.getLong(
                                                "pending_installments"
                                        ),
                                        resultSet.getLong(
                                                "partially_paid_installments"
                                        ),
                                        resultSet.getLong(
                                                "paid_installments"
                                        ),
                                        resultSet.getLong(
                                                "overdue_installments"
                                        ),
                                        resultSet.getLong(
                                                "cancelled_installments"
                                        ),
                                        money(
                                                resultSet,
                                                "scheduled_amount"
                                        ),
                                        money(
                                                resultSet,
                                                "paid_amount"
                                        ),
                                        money(
                                                resultSet,
                                                "outstanding_amount"
                                        )
                                )
                );

        if (result == null) {
            return new InstallmentSummary(
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    ZERO,
                    ZERO,
                    ZERO
            );
        }

        return result;
    }

    private FinancialAccountReportResponse.AdjustmentSummary
    findAccountAdjustments(
            MapSqlParameterSource parameters
    ) {
        FinancialAccountReportResponse.AdjustmentSummary
                result =
                jdbcTemplate.queryForObject(
                        """
                        SELECT
                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN type = 'DISCOUNT'
                                        THEN 1
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS discount_count,

                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN type = 'DISCOUNT'
                                        THEN amount
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS discount_amount,

                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN type = 'CHARGE'
                                        THEN 1
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS charge_count,

                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN type = 'CHARGE'
                                        THEN amount
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS charge_amount

                        FROM financial_adjustments
                        WHERE financial_account_id =
                              :financialAccountId
                        """,
                        parameters,
                        (resultSet, rowNumber) ->
                                new FinancialAccountReportResponse
                                        .AdjustmentSummary(
                                        resultSet.getLong(
                                                "discount_count"
                                        ),
                                        money(
                                                resultSet,
                                                "discount_amount"
                                        ),
                                        resultSet.getLong(
                                                "charge_count"
                                        ),
                                        money(
                                                resultSet,
                                                "charge_amount"
                                        )
                                )
                );

        if (result == null) {
            return new FinancialAccountReportResponse
                    .AdjustmentSummary(
                    0,
                    ZERO,
                    0,
                    ZERO
            );
        }

        return result;
    }

    private CountAmount queryCountAmount(
            String sql,
            MapSqlParameterSource parameters
    ) {
        CountAmount result =
                jdbcTemplate.queryForObject(
                        sql,
                        parameters,
                        (resultSet, rowNumber) ->
                                new CountAmount(
                                        resultSet.getLong(
                                                "item_count"
                                        ),
                                        money(
                                                resultSet,
                                                "total_amount"
                                        )
                                )
                );

        if (result == null) {
            return CountAmount.empty();
        }

        return result;
    }

    private MapSqlParameterSource
    createPeriodParameters(
            String currency,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return new MapSqlParameterSource()
                .addValue("currency", currency)
                .addValue("fromDate", fromDate)
                .addValue("toDate", toDate)
                .addValue(
                        "fromInstant",
                        fromDate
                                .atStartOfDay()
                                .atOffset(
                                        ZoneOffset.UTC
                                )
                )
                .addValue(
                        "toExclusiveInstant",
                        toDate
                                .plusDays(1)
                                .atStartOfDay()
                                .atOffset(
                                        ZoneOffset.UTC
                                )
                );
    }

    private BigDecimal money(
            ResultSet resultSet,
            String column
    ) throws SQLException {
        BigDecimal value =
                resultSet.getBigDecimal(column);

        return value == null ? ZERO : value;
    }

    private BigDecimal positiveDifference(
            BigDecimal first,
            BigDecimal second
    ) {
        return first
                .subtract(second)
                .max(ZERO);
    }

    private UUID readUuid(
            ResultSet resultSet,
            String column
    ) throws SQLException {
        return resultSet.getObject(
                column,
                UUID.class
        );
    }

    private record AccountPortfolioBase(

            long totalAccounts,
            long openAccounts,
            long settledAccounts,
            long cancelledAccounts,
            BigDecimal billedAmount,
            BigDecimal paidAmount

    ) {

        private static AccountPortfolioBase empty() {
            return new AccountPortfolioBase(
                    0,
                    0,
                    0,
                    0,
                    ZERO,
                    ZERO
            );
        }
    }

    private record CountAmount(

            long count,
            BigDecimal amount

    ) {

        private static CountAmount empty() {
            return new CountAmount(
                    0,
                    ZERO
            );
        }
    }

    private record AccountDetails(

            UUID financialAccountId,
            UUID enrollmentId,
            UUID studentId,
            String pricingPlanCode,
            String status,
            String currency,
            BigDecimal totalAmount,
            BigDecimal paidAmount

    ) {
    }
}