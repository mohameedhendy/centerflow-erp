package com.centerflow.finance.pricing.domain;

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
@Table(name = "pricing_plans")
public class PricingPlan {

    private static final int MAX_CODE_LENGTH = 30;
    private static final int MAX_NAME_LENGTH = 150;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int MAX_INSTALLMENT_COUNT = 60;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(
            name = "code",
            nullable = false,
            unique = true,
            length = MAX_CODE_LENGTH
    )
    private String code;

    @Column(
            name = "name",
            nullable = false,
            length = MAX_NAME_LENGTH
    )
    private String name;

    @Column(
            name = "description",
            length = MAX_DESCRIPTION_LENGTH
    )
    private String description;

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
    private PricingPlanStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PricingPlan() {
    }

    private PricingPlan(
            UUID id,
            String code,
            String name,
            String description,
            BigDecimal totalAmount,
            String currency,
            int installmentCount,
            BigDecimal initialPaymentAmount,
            PricingPlanStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.code = normalizeCode(code);
        this.name = normalizeRequiredText(
                name,
                "Pricing plan name",
                MAX_NAME_LENGTH
        );
        this.description = normalizeDescription(description);
        this.totalAmount = normalizeTotalAmount(totalAmount);
        this.currency = normalizeCurrency(currency);
        this.installmentCount =
                validateInstallmentCount(installmentCount);
        this.initialPaymentAmount =
                normalizeInitialPaymentAmount(
                        initialPaymentAmount,
                        this.totalAmount
                );
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static PricingPlan create(
            String code,
            String name,
            String description,
            BigDecimal totalAmount,
            String currency,
            int installmentCount,
            BigDecimal initialPaymentAmount
    ) {
        Instant now = Instant.now();

        return new PricingPlan(
                UUID.randomUUID(),
                code,
                name,
                description,
                totalAmount,
                currency,
                installmentCount,
                initialPaymentAmount,
                PricingPlanStatus.ACTIVE,
                now,
                now
        );
    }

    public void activate() {
        if (status == PricingPlanStatus.ACTIVE) {
            return;
        }

        status = PricingPlanStatus.ACTIVE;
        updatedAt = Instant.now();
    }

    public void deactivate() {
        if (status == PricingPlanStatus.INACTIVE) {
            return;
        }

        status = PricingPlanStatus.INACTIVE;
        updatedAt = Instant.now();
    }

    private static String normalizeCode(String code) {
        return normalizeRequiredText(
                code,
                "Pricing plan code",
                MAX_CODE_LENGTH
        ).toUpperCase(Locale.ROOT);
    }

    private static String normalizeDescription(
            String description
    ) {
        if (description == null || description.isBlank()) {
            return null;
        }

        String normalized = description.trim();

        if (normalized.length() > MAX_DESCRIPTION_LENGTH) {
            throw new InvalidPricingPlanException(
                    "Pricing plan description must not exceed "
                            + MAX_DESCRIPTION_LENGTH
                            + " characters"
            );
        }

        return normalized;
    }

    private static String normalizeRequiredText(
            String value,
            String fieldName,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            throw new InvalidPricingPlanException(
                    fieldName + " is required"
            );
        }

        String normalized = value.trim();

        if (normalized.length() > maxLength) {
            throw new InvalidPricingPlanException(
                    fieldName
                            + " must not exceed "
                            + maxLength
                            + " characters"
            );
        }

        return normalized;
    }

    private static BigDecimal normalizeTotalAmount(
            BigDecimal amount
    ) {
        BigDecimal normalized = normalizeMoney(
                amount,
                "Total amount"
        );

        if (normalized.signum() <= 0) {
            throw new InvalidPricingPlanException(
                    "Total amount must be greater than zero"
            );
        }

        return normalized;
    }

    private static BigDecimal normalizeInitialPaymentAmount(
            BigDecimal amount,
            BigDecimal totalAmount
    ) {
        BigDecimal normalized = normalizeMoney(
                amount,
                "Initial payment amount"
        );

        if (normalized.signum() < 0) {
            throw new InvalidPricingPlanException(
                    "Initial payment amount cannot be negative"
            );
        }

        if (normalized.compareTo(totalAmount) > 0) {
            throw new InvalidPricingPlanException(
                    "Initial payment amount cannot exceed total amount"
            );
        }

        return normalized;
    }

    private static BigDecimal normalizeMoney(
            BigDecimal amount,
            String fieldName
    ) {
        if (amount == null) {
            throw new InvalidPricingPlanException(
                    fieldName + " is required"
            );
        }

        try {
            return amount.setScale(
                    2,
                    RoundingMode.UNNECESSARY
            );
        } catch (ArithmeticException exception) {
            throw new InvalidPricingPlanException(
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
            throw new InvalidPricingPlanException(
                    "Currency is required"
            );
        }

        String normalized = currency
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z]{3}")) {
            throw new InvalidPricingPlanException(
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
                        || installmentCount > MAX_INSTALLMENT_COUNT
        ) {
            throw new InvalidPricingPlanException(
                    "Installment count must be between 1 and "
                            + MAX_INSTALLMENT_COUNT
            );
        }

        return installmentCount;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
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

    public PricingPlanStatus getStatus() {
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