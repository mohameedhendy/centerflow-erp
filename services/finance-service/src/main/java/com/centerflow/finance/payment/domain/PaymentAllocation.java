package com.centerflow.finance.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "payment_allocations")
public class PaymentAllocation {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "installment_id", nullable = false)
    private UUID installmentId;

    @Column(name = "allocation_order", nullable = false)
    private int allocationOrder;

    @Column(
            name = "amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PaymentAllocation() {
    }

    private PaymentAllocation(
            UUID id,
            UUID paymentId,
            UUID installmentId,
            int allocationOrder,
            BigDecimal amount,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.paymentId = Objects.requireNonNull(paymentId);
        this.installmentId =
                Objects.requireNonNull(installmentId);

        if (allocationOrder < 1) {
            throw new IllegalArgumentException(
                    "Allocation order must be one or greater"
            );
        }

        this.allocationOrder = allocationOrder;
        this.amount = normalizeAmount(amount);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static PaymentAllocation create(
            UUID paymentId,
            UUID installmentId,
            int allocationOrder,
            BigDecimal amount
    ) {
        return new PaymentAllocation(
                UUID.randomUUID(),
                paymentId,
                installmentId,
                allocationOrder,
                amount,
                Instant.now()
        );
    }

    private static BigDecimal normalizeAmount(
            BigDecimal amount
    ) {
        Objects.requireNonNull(amount);

        try {
            BigDecimal normalized = amount.setScale(
                    2,
                    RoundingMode.UNNECESSARY
            );

            if (normalized.signum() <= 0) {
                throw new IllegalArgumentException(
                        "Allocation amount must be greater than zero"
                );
            }

            return normalized;
        }
        catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Allocation amount must have no more "
                            + "than two decimal places"
            );
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getInstallmentId() {
        return installmentId;
    }

    public int getAllocationOrder() {
        return allocationOrder;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}