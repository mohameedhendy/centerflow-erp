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
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "installments")
public class Installment {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(
            name = "financial_account_id",
            nullable = false
    )
    private UUID financialAccountId;

    @Column(name = "installment_number", nullable = false)
    private int installmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(
            name = "amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal amount;

    @Column(
            name = "paid_amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InstallmentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Installment() {
    }

    private Installment(
            UUID id,
            UUID financialAccountId,
            int installmentNumber,
            LocalDate dueDate,
            BigDecimal amount,
            BigDecimal paidAmount,
            InstallmentStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.financialAccountId =
                Objects.requireNonNull(financialAccountId);

        if (installmentNumber < 1) {
            throw new IllegalArgumentException(
                    "Installment number must be one or greater"
            );
        }

        this.installmentNumber = installmentNumber;
        this.dueDate = Objects.requireNonNull(dueDate);
        this.amount = normalizePositiveMoney(amount);
        this.paidAmount = normalizeMoney(paidAmount);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static Installment create(
            UUID financialAccountId,
            int installmentNumber,
            LocalDate dueDate,
            BigDecimal amount
    ) {
        Instant now = Instant.now();

        return new Installment(
                UUID.randomUUID(),
                financialAccountId,
                installmentNumber,
                dueDate,
                amount,
                new BigDecimal("0.00"),
                InstallmentStatus.PENDING,
                now,
                now
        );
    }

    private static BigDecimal normalizePositiveMoney(
            BigDecimal value
    ) {
        BigDecimal normalized = normalizeMoney(value);

        if (normalized.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Installment amount must be greater than zero"
            );
        }

        return normalized;
    }

    private static BigDecimal normalizeMoney(
            BigDecimal value
    ) {
        Objects.requireNonNull(value);

        try {
            return value.setScale(
                    2,
                    RoundingMode.UNNECESSARY
            );
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Money amount must have no more than "
                            + "two decimal places"
            );
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getFinancialAccountId() {
        return financialAccountId;
    }

    public int getInstallmentNumber() {
        return installmentNumber;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public InstallmentStatus getStatus() {
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