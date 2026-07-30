package com.centerflow.finance.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "enrollment_financial_accounts")
public class EnrollmentFinancialAccount {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(
            name = "enrollment_id",
            nullable = false,
            unique = true
    )
    private UUID enrollmentId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "pricing_plan_id", nullable = false)
    private UUID pricingPlanId;

    @Column(
            name = "pricing_plan_code",
            nullable = false,
            length = 30
    )
    private String pricingPlanCode;

    @Column(
            name = "total_amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal totalAmount;

    @Column(
            name = "currency",
            nullable = false,
            length = 3
    )
    private String currency;

    @Column(name = "installment_count", nullable = false)
    private int installmentCount;

    @Column(
            name = "initial_payment_amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal initialPaymentAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FinancialAccountStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected EnrollmentFinancialAccount() {
    }

    private EnrollmentFinancialAccount(
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
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.enrollmentId =
                Objects.requireNonNull(enrollmentId);
        this.studentId = Objects.requireNonNull(studentId);
        this.pricingPlanId =
                Objects.requireNonNull(pricingPlanId);
        this.pricingPlanCode =
                normalizeRequiredText(pricingPlanCode);
        this.totalAmount =
                normalizePositiveMoney(totalAmount);
        this.currency = normalizeCurrency(currency);
        this.installmentCount =
                validateInstallmentCount(installmentCount);
        this.initialPaymentAmount =
                normalizeInitialPayment(
                        initialPaymentAmount,
                        this.totalAmount
                );
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static EnrollmentFinancialAccount create(
            UUID enrollmentId,
            UUID studentId,
            UUID pricingPlanId,
            String pricingPlanCode,
            BigDecimal totalAmount,
            String currency,
            int installmentCount,
            BigDecimal initialPaymentAmount
    ) {
        Instant now = Instant.now();

        return new EnrollmentFinancialAccount(
                UUID.randomUUID(),
                enrollmentId,
                studentId,
                pricingPlanId,
                pricingPlanCode,
                totalAmount,
                currency,
                installmentCount,
                initialPaymentAmount,
                FinancialAccountStatus.OPEN,
                now,
                now
        );
    }

    private static String normalizeRequiredText(
            String value
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Pricing plan code is required"
            );
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static BigDecimal normalizePositiveMoney(
            BigDecimal value
    ) {
        BigDecimal normalized = normalizeMoney(
                value,
                "Total amount"
        );

        if (normalized.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Total amount must be greater than zero"
            );
        }

        return normalized;
    }

    private static BigDecimal normalizeInitialPayment(
            BigDecimal value,
            BigDecimal totalAmount
    ) {
        BigDecimal normalized = normalizeMoney(
                value,
                "Initial payment amount"
        );

        if (normalized.signum() < 0) {
            throw new IllegalArgumentException(
                    "Initial payment amount cannot be negative"
            );
        }

        if (normalized.compareTo(totalAmount) > 0) {
            throw new IllegalArgumentException(
                    "Initial payment amount cannot exceed total amount"
            );
        }

        return normalized;
    }

    private static BigDecimal normalizeMoney(
            BigDecimal value,
            String fieldName
    ) {
        Objects.requireNonNull(
                value,
                fieldName + " is required"
        );

        try {
            return value.setScale(
                    2,
                    RoundingMode.UNNECESSARY
            );
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must have no more than "
                            + "two decimal places"
            );
        }
    }

    private static String normalizeCurrency(
            String currency
    ) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException(
                    "Currency is required"
            );
        }

        String normalized = currency
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "Currency must contain exactly three letters"
            );
        }

        return normalized;
    }

    private static int validateInstallmentCount(
            int installmentCount
    ) {
        if (
                installmentCount < 1
                        || installmentCount > 60
        ) {
            throw new IllegalArgumentException(
                    "Installment count must be between 1 and 60"
            );
        }

        return installmentCount;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEnrollmentId() {
        return enrollmentId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getPricingPlanId() {
        return pricingPlanId;
    }

    public String getPricingPlanCode() {
        return pricingPlanCode;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public int getInstallmentCount() {
        return installmentCount;
    }

    public BigDecimal getInitialPaymentAmount() {
        return initialPaymentAmount;
    }

    public FinancialAccountStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}