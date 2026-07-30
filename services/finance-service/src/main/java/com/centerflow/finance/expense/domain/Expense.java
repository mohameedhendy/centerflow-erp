package com.centerflow.finance.expense.domain;

import com.centerflow.finance.expense.exception.ExpenseConflictException;
import com.centerflow.finance.expense.exception.InvalidExpenseException;
import com.centerflow.finance.payment.domain.PaymentMethod;
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
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(
            name = "expense_number",
            nullable = false,
            unique = true,
            length = 30
    )
    private String expenseNumber;

    @Column(name = "branch_id")
    private UUID branchId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "category",
            nullable = false,
            length = 30
    )
    private ExpenseCategory category;

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
    @Column(
            name = "payment_method",
            nullable = false,
            length = 30
    )
    private PaymentMethod paymentMethod;

    @Column(
            name = "payee",
            nullable = false,
            length = 150
    )
    private String payee;

    @Column(
            name = "description",
            nullable = false,
            length = 500
    )
    private String description;

    @Column(
            name = "expense_date",
            nullable = false
    )
    private LocalDate expenseDate;

    @Column(
            name = "external_reference",
            unique = true,
            length = 100
    )
    private String externalReference;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private ExpenseStatus status;

    @Column(
            name = "cancellation_reason",
            length = 500
    )
    private String cancellationReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Expense() {
    }

    private Expense(
            UUID id,
            String expenseNumber,
            UUID branchId,
            ExpenseCategory category,
            BigDecimal amount,
            String currency,
            PaymentMethod paymentMethod,
            String payee,
            String description,
            LocalDate expenseDate,
            String externalReference,
            ExpenseStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);

        this.expenseNumber =
                normalizeRequiredText(
                        expenseNumber,
                        "Expense number",
                        3,
                        30
                );

        this.branchId = branchId;

        this.category = Objects.requireNonNull(
                category,
                "Expense category is required"
        );

        this.amount = normalizeAmount(amount);
        this.currency = normalizeCurrency(currency);

        this.paymentMethod = Objects.requireNonNull(
                paymentMethod,
                "Expense payment method is required"
        );

        this.payee = normalizeRequiredText(
                payee,
                "Expense payee",
                2,
                150
        );

        this.description = normalizeRequiredText(
                description,
                "Expense description",
                3,
                500
        );

        this.expenseDate =
                validateExpenseDate(expenseDate);

        this.externalReference =
                normalizeOptionalText(
                        externalReference,
                        "Expense external reference",
                        100
                );

        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static Expense create(
            String expenseNumber,
            UUID branchId,
            ExpenseCategory category,
            BigDecimal amount,
            String currency,
            PaymentMethod paymentMethod,
            String payee,
            String description,
            LocalDate expenseDate,
            String externalReference
    ) {
        Instant now = Instant.now();

        return new Expense(
                UUID.randomUUID(),
                expenseNumber,
                branchId,
                category,
                amount,
                currency,
                paymentMethod,
                payee,
                description,
                expenseDate,
                externalReference,
                ExpenseStatus.RECORDED,
                now,
                now
        );
    }

    public void cancel(String reason) {
        if (status == ExpenseStatus.CANCELLED) {
            return;
        }

        cancellationReason =
                normalizeRequiredText(
                        reason,
                        "Expense cancellation reason",
                        3,
                        500
                );

        status = ExpenseStatus.CANCELLED;
        cancelledAt = Instant.now();
        updatedAt = cancelledAt;
    }

    private static BigDecimal normalizeAmount(
            BigDecimal value
    ) {
        Objects.requireNonNull(
                value,
                "Expense amount is required"
        );

        BigDecimal normalized;

        try {
            normalized = value.setScale(
                    2,
                    RoundingMode.UNNECESSARY
            );
        }
        catch (ArithmeticException exception) {
            throw new InvalidExpenseException(
                    "Expense amount must have no more "
                            + "than two decimal places"
            );
        }

        if (normalized.signum() <= 0) {
            throw new InvalidExpenseException(
                    "Expense amount must be greater than zero"
            );
        }

        return normalized;
    }

    private static String normalizeCurrency(
            String value
    ) {
        if (value == null || value.isBlank()) {
            throw new InvalidExpenseException(
                    "Expense currency is required"
            );
        }

        String normalized = value
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z]{3}")) {
            throw new InvalidExpenseException(
                    "Expense currency must contain "
                            + "exactly three letters"
            );
        }

        return normalized;
    }

    private static LocalDate validateExpenseDate(
            LocalDate value
    ) {
        Objects.requireNonNull(
                value,
                "Expense date is required"
        );

        if (value.isAfter(LocalDate.now(ZoneOffset.UTC))) {
            throw new InvalidExpenseException(
                    "Expense date cannot be in the future"
            );
        }

        return value;
    }

    private static String normalizeRequiredText(
            String value,
            String fieldName,
            int minimumLength,
            int maximumLength
    ) {
        if (value == null || value.isBlank()) {
            throw new InvalidExpenseException(
                    fieldName + " is required"
            );
        }

        String normalized = value.trim();

        if (
                normalized.length() < minimumLength
                        || normalized.length() > maximumLength
        ) {
            throw new InvalidExpenseException(
                    fieldName
                            + " must be between "
                            + minimumLength
                            + " and "
                            + maximumLength
                            + " characters"
            );
        }

        return normalized;
    }

    private static String normalizeOptionalText(
            String value,
            String fieldName,
            int maximumLength
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.length() > maximumLength) {
            throw new InvalidExpenseException(
                    fieldName
                            + " must not exceed "
                            + maximumLength
                            + " characters"
            );
        }

        return normalized;
    }

    public UUID getId() {
        return id;
    }

    public String getExpenseNumber() {
        return expenseNumber;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getPayee() {
        return payee;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public ExpenseStatus getStatus() {
        return status;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public long getVersion() {
        return version;
    }
}