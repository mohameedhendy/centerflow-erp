package com.centerflow.finance.refund.domain;

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
@Table(name = "refunds")
public class Refund {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(
            name = "refund_number",
            nullable = false,
            unique = true,
            length = 30
    )
    private String refundNumber;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RefundStatus status;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Refund() {
    }

    private Refund(
            UUID id,
            String refundNumber,
            UUID paymentId,
            BigDecimal amount,
            String currency,
            String reason,
            String externalReference,
            RefundStatus status,
            Instant recordedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.refundNumber =
                normalizeRefundNumber(refundNumber);
        this.paymentId = Objects.requireNonNull(paymentId);
        this.amount = normalizeAmount(amount);
        this.currency = normalizeCurrency(currency);
        this.reason = normalizeReason(reason);
        this.externalReference =
                normalizeExternalReference(
                        externalReference
                );
        this.status = Objects.requireNonNull(status);
        this.recordedAt = Objects.requireNonNull(recordedAt);
    }

    public static Refund create(
            String refundNumber,
            UUID paymentId,
            BigDecimal amount,
            String currency,
            String reason,
            String externalReference
    ) {
        return new Refund(
                UUID.randomUUID(),
                refundNumber,
                paymentId,
                amount,
                currency,
                reason,
                externalReference,
                RefundStatus.RECORDED,
                Instant.now()
        );
    }

    private static String normalizeRefundNumber(
            String refundNumber
    ) {
        if (
                refundNumber == null
                        || refundNumber.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Refund number is required"
            );
        }

        return refundNumber
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private static BigDecimal normalizeAmount(
            BigDecimal amount
    ) {
        Objects.requireNonNull(
                amount,
                "Refund amount is required"
        );

        BigDecimal normalized;

        try {
            normalized = amount.setScale(
                    2,
                    RoundingMode.UNNECESSARY
            );
        }
        catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Refund amount must have no more "
                            + "than two decimal places"
            );
        }

        if (normalized.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Refund amount must be greater than zero"
            );
        }

        return normalized;
    }

    private static String normalizeCurrency(
            String currency
    ) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException(
                    "Currency is required"
            );
        }

        return currency
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private static String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Refund reason is required"
            );
        }

        String normalized = reason.trim();

        if (normalized.length() > 500) {
            throw new IllegalArgumentException(
                    "Refund reason must not exceed "
                            + "500 characters"
            );
        }

        return normalized;
    }

    private static String normalizeExternalReference(
            String externalReference
    ) {
        if (
                externalReference == null
                        || externalReference.isBlank()
        ) {
            return null;
        }

        return externalReference.trim();
    }

    public UUID getId() {
        return id;
    }

    public String getRefundNumber() {
        return refundNumber;
    }

    public UUID getPaymentId() {
        return paymentId;
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

    public RefundStatus getStatus() {
        return status;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public long getVersion() {
        return version;
    }
}