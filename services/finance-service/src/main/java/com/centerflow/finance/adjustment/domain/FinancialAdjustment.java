package com.centerflow.finance.adjustment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "financial_adjustments")
public class FinancialAdjustment {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(
            name = "financial_account_id",
            nullable = false
    )
    private UUID financialAccountId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "type",
            nullable = false,
            length = 20
    )
    private FinancialAdjustmentType type;

    @Column(
            name = "amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal amount;

    @Column(
            name = "currency",
            nullable = false,
            length = 3
    )
    private String currency;

    @Column(
            name = "reason",
            nullable = false,
            length = 500
    )
    private String reason;

    @Column(
            name = "external_reference",
            unique = true,
            length = 100
    )
    private String externalReference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FinancialAdjustment() {
    }

    private FinancialAdjustment(
            UUID id,
            UUID financialAccountId,
            FinancialAdjustmentType type,
            BigDecimal amount,
            String currency,
            String reason,
            String externalReference,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id);

        this.financialAccountId =
                Objects.requireNonNull(
                        financialAccountId
                );

        this.type = Objects.requireNonNull(
                type,
                "Adjustment type is required"
        );

        this.amount = normalizeAmount(amount);
        this.currency = normalizeCurrency(currency);
        this.reason = normalizeReason(reason);

        this.externalReference =
                normalizeExternalReference(
                        externalReference
                );

        this.createdAt =
                Objects.requireNonNull(createdAt);
    }

    public static FinancialAdjustment create(
            UUID financialAccountId,
            FinancialAdjustmentType type,
            BigDecimal amount,
            String currency,
            String reason,
            String externalReference
    ) {
        return new FinancialAdjustment(
                UUID.randomUUID(),
                financialAccountId,
                type,
                amount,
                currency,
                reason,
                externalReference,
                Instant.now()
        );
    }

    private static BigDecimal normalizeAmount(
            BigDecimal value
    ) {
        Objects.requireNonNull(
                value,
                "Adjustment amount is required"
        );

        BigDecimal normalized;

        try {
            normalized = value.setScale(
                    2,
                    RoundingMode.UNNECESSARY
            );
        }
        catch (ArithmeticException exception) {
            throw new InvalidFinancialAdjustmentException(
                    "Adjustment amount must have no more "
                            + "than two decimal places"
            );
        }

        if (normalized.signum() <= 0) {
            throw new InvalidFinancialAdjustmentException(
                    "Adjustment amount must be greater than zero"
            );
        }

        return normalized;
    }

    private static String normalizeCurrency(
            String value
    ) {
        if (value == null || value.isBlank()) {
            throw new InvalidFinancialAdjustmentException(
                    "Adjustment currency is required"
            );
        }

        String normalized = value
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z]{3}")) {
            throw new InvalidFinancialAdjustmentException(
                    "Adjustment currency must contain "
                            + "exactly three letters"
            );
        }

        return normalized;
    }

    private static String normalizeReason(
            String value
    ) {
        if (value == null || value.isBlank()) {
            throw new InvalidFinancialAdjustmentException(
                    "Adjustment reason is required"
            );
        }

        String normalized = value.trim();

        if (
                normalized.length() < 3
                        || normalized.length() > 500
        ) {
            throw new InvalidFinancialAdjustmentException(
                    "Adjustment reason must be between "
                            + "3 and 500 characters"
            );
        }

        return normalized;
    }

    private static String normalizeExternalReference(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.length() > 100) {
            throw new InvalidFinancialAdjustmentException(
                    "Adjustment external reference "
                            + "must not exceed 100 characters"
            );
        }

        return normalized;
    }

    public UUID getId() {
        return id;
    }

    public UUID getFinancialAccountId() {
        return financialAccountId;
    }

    public FinancialAdjustmentType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getReason() {
        return reason;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}