package com.centerflow.finance.refund.domain;

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
@Table(name = "refund_allocations")
public class RefundAllocation {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "refund_id", nullable = false)
    private UUID refundId;

    @Column(
            name = "payment_allocation_id",
            nullable = false
    )
    private UUID paymentAllocationId;

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

    protected RefundAllocation() {
    }

    private RefundAllocation(
            UUID id,
            UUID refundId,
            UUID paymentAllocationId,
            UUID installmentId,
            int allocationOrder,
            BigDecimal amount,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.refundId = Objects.requireNonNull(refundId);
        this.paymentAllocationId =
                Objects.requireNonNull(paymentAllocationId);
        this.installmentId =
                Objects.requireNonNull(installmentId);

        if (allocationOrder < 1) {
            throw new IllegalArgumentException(
                    "Refund allocation order must be "
                            + "one or greater"
            );
        }

        this.allocationOrder = allocationOrder;
        this.amount = normalizeAmount(amount);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static RefundAllocation create(
            UUID refundId,
            UUID paymentAllocationId,
            UUID installmentId,
            int allocationOrder,
            BigDecimal amount
    ) {
        return new RefundAllocation(
                UUID.randomUUID(),
                refundId,
                paymentAllocationId,
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
                        "Refund allocation amount must "
                                + "be greater than zero"
                );
            }

            return normalized;
        }
        catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Refund allocation amount must have "
                            + "no more than two decimal places"
            );
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getRefundId() {
        return refundId;
    }

    public UUID getPaymentAllocationId() {
        return paymentAllocationId;
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