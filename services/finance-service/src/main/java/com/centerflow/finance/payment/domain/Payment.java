package com.centerflow.finance.payment.domain;

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
@Table(name = "payments")
public class Payment {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(
            name = "payment_number",
            nullable = false,
            unique = true,
            length = 30
    )
    private String paymentNumber;

    @Column(
            name = "financial_account_id",
            nullable = false
    )
    private UUID financialAccountId;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 30)
    private PaymentMethod method;

    @Column(
            name = "external_reference",
            unique = true,
            length = 100
    )
    private String externalReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Payment() {
    }

    private Payment(
            UUID id,
            String paymentNumber,
            UUID financialAccountId,
            BigDecimal amount,
            String currency,
            PaymentMethod method,
            String externalReference,
            PaymentStatus status,
            Instant recordedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.paymentNumber =
                normalizePaymentNumber(paymentNumber);
        this.financialAccountId =
                Objects.requireNonNull(financialAccountId);
        this.amount = normalizeAmount(amount);
        this.currency = normalizeCurrency(currency);
        this.method = Objects.requireNonNull(method);
        this.externalReference =
                normalizeExternalReference(
                        externalReference
                );
        this.status = Objects.requireNonNull(status);
        this.recordedAt =
                Objects.requireNonNull(recordedAt);
    }

    public static Payment create(
            String paymentNumber,
            UUID financialAccountId,
            BigDecimal amount,
            String currency,
            PaymentMethod method,
            String externalReference
    ) {
        return new Payment(
                UUID.randomUUID(),
                paymentNumber,
                financialAccountId,
                amount,
                currency,
                method,
                externalReference,
                PaymentStatus.RECORDED,
                Instant.now()
        );
    }

    private static String normalizePaymentNumber(
            String paymentNumber
    ) {
        if (
                paymentNumber == null
                        || paymentNumber.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Payment number is required"
            );
        }

        return paymentNumber
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private static BigDecimal normalizeAmount(
            BigDecimal amount
    ) {
        Objects.requireNonNull(
                amount,
                "Payment amount is required"
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
                    "Payment amount must have no more "
                            + "than two decimal places"
            );
        }

        if (normalized.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero"
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

    public String getPaymentNumber() {
        return paymentNumber;
    }

    public UUID getFinancialAccountId() {
        return financialAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public long getVersion() {
        return version;
    }
}