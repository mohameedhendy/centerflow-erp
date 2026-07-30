package com.centerflow.finance.earning.domain;

import com.centerflow.finance.earning.exception.InstructorEarningConflictException;
import com.centerflow.finance.earning.exception.InvalidInstructorEarningException;
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
@Table(name = "instructor_earnings")
public class InstructorEarning {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(
            name = "earning_number",
            nullable = false,
            unique = true,
            length = 30
    )
    private String earningNumber;

    @Column(
            name = "instructor_id",
            nullable = false
    )
    private UUID instructorId;

    @Column(
            name = "session_id",
            nullable = false,
            unique = true
    )
    private UUID sessionId;

    @Column(
            name = "batch_id",
            nullable = false
    )
    private UUID batchId;

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
            name = "session_date",
            nullable = false
    )
    private LocalDate sessionDate;

    @Column(
            name = "description",
            nullable = false,
            length = 500
    )
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private InstructorEarningStatus status;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_method",
            length = 30
    )
    private PaymentMethod paymentMethod;

    @Column(
            name = "payment_reference",
            unique = true,
            length = 100
    )
    private String paymentReference;

    @Column(
            name = "cancellation_reason",
            length = 500
    )
    private String cancellationReason;

    @Column(
            name = "accrued_at",
            nullable = false
    )
    private Instant accruedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected InstructorEarning() {
    }

    private InstructorEarning(
            UUID id,
            String earningNumber,
            UUID instructorId,
            UUID sessionId,
            UUID batchId,
            BigDecimal amount,
            String currency,
            LocalDate sessionDate,
            String description,
            Instant now
    ) {
        this.id = Objects.requireNonNull(id);

        this.earningNumber = normalizeRequiredText(
                earningNumber,
                "Earning number",
                3,
                30
        );

        this.instructorId = Objects.requireNonNull(
                instructorId,
                "Instructor ID is required"
        );

        this.sessionId = Objects.requireNonNull(
                sessionId,
                "Session ID is required"
        );

        this.batchId = Objects.requireNonNull(
                batchId,
                "Batch ID is required"
        );

        this.amount = normalizeAmount(amount);
        this.currency = normalizeCurrency(currency);
        this.sessionDate = validateSessionDate(sessionDate);

        this.description = normalizeRequiredText(
                description,
                "Earning description",
                3,
                500
        );

        this.status = InstructorEarningStatus.ACCRUED;
        this.accruedAt = Objects.requireNonNull(now);
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static InstructorEarning create(
            String earningNumber,
            UUID instructorId,
            UUID sessionId,
            UUID batchId,
            BigDecimal amount,
            String currency,
            LocalDate sessionDate,
            String description
    ) {
        return new InstructorEarning(
                UUID.randomUUID(),
                earningNumber,
                instructorId,
                sessionId,
                batchId,
                amount,
                currency,
                sessionDate,
                description,
                Instant.now()
        );
    }

    public void markPaid(
            PaymentMethod paymentMethod,
            String paymentReference
    ) {
        if (status == InstructorEarningStatus.PAID) {
            return;
        }

        if (status == InstructorEarningStatus.CANCELLED) {
            throw new InstructorEarningConflictException(
                    "Cancelled instructor earning "
                            + "cannot be paid"
            );
        }

        this.paymentMethod = Objects.requireNonNull(
                paymentMethod,
                "Payment method is required"
        );

        this.paymentReference = normalizeRequiredText(
                paymentReference,
                "Payment reference",
                3,
                100
        );

        Instant now = Instant.now();

        status = InstructorEarningStatus.PAID;
        paidAt = now;
        updatedAt = now;
    }

    public void cancel(String reason) {
        if (status == InstructorEarningStatus.CANCELLED) {
            return;
        }

        if (status == InstructorEarningStatus.PAID) {
            throw new InstructorEarningConflictException(
                    "Paid instructor earning "
                            + "cannot be cancelled"
            );
        }

        cancellationReason = normalizeRequiredText(
                reason,
                "Cancellation reason",
                3,
                500
        );

        Instant now = Instant.now();

        status = InstructorEarningStatus.CANCELLED;
        cancelledAt = now;
        updatedAt = now;
    }

    private static BigDecimal normalizeAmount(
            BigDecimal value
    ) {
        Objects.requireNonNull(
                value,
                "Earning amount is required"
        );

        BigDecimal normalized;

        try {
            normalized = value.setScale(
                    2,
                    RoundingMode.UNNECESSARY
            );
        }
        catch (ArithmeticException exception) {
            throw new InvalidInstructorEarningException(
                    "Earning amount must have no more "
                            + "than two decimal places"
            );
        }

        if (normalized.signum() <= 0) {
            throw new InvalidInstructorEarningException(
                    "Earning amount must be greater than zero"
            );
        }

        return normalized;
    }

    private static String normalizeCurrency(
            String value
    ) {
        if (value == null || value.isBlank()) {
            throw new InvalidInstructorEarningException(
                    "Earning currency is required"
            );
        }

        String normalized = value
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z]{3}")) {
            throw new InvalidInstructorEarningException(
                    "Earning currency must contain "
                            + "exactly three letters"
            );
        }

        return normalized;
    }

    private static LocalDate validateSessionDate(
            LocalDate value
    ) {
        Objects.requireNonNull(
                value,
                "Session date is required"
        );

        if (value.isAfter(LocalDate.now(ZoneOffset.UTC))) {
            throw new InvalidInstructorEarningException(
                    "Session date cannot be in the future"
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
            throw new InvalidInstructorEarningException(
                    fieldName + " is required"
            );
        }

        String normalized = value.trim();

        if (
                normalized.length() < minimumLength
                        || normalized.length() > maximumLength
        ) {
            throw new InvalidInstructorEarningException(
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

    public UUID getId() {
        return id;
    }

    public String getEarningNumber() {
        return earningNumber;
    }

    public UUID getInstructorId() {
        return instructorId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public String getDescription() {
        return description;
    }

    public InstructorEarningStatus getStatus() {
        return status;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public Instant getAccruedAt() {
        return accruedAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
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