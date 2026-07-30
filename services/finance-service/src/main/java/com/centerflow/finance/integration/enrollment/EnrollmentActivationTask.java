package com.centerflow.finance.integration.enrollment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "enrollment_activation_tasks")
public class EnrollmentActivationTask {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(
            name = "enrollment_id",
            nullable = false,
            unique = true
    )
    private UUID enrollmentId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private EnrollmentActivationStatus status;

    @Column(
            name = "attempt_count",
            nullable = false
    )
    private int attemptCount;

    @Column(
            name = "last_error",
            length = 1000
    )
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected EnrollmentActivationTask() {
    }

    private EnrollmentActivationTask(
            UUID id,
            UUID enrollmentId,
            UUID paymentId,
            EnrollmentActivationStatus status,
            int attemptCount,
            String lastError,
            Instant createdAt,
            Instant updatedAt,
            Instant completedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.enrollmentId =
                Objects.requireNonNull(enrollmentId);
        this.paymentId =
                Objects.requireNonNull(paymentId);
        this.status = Objects.requireNonNull(status);
        this.attemptCount = attemptCount;
        this.lastError = lastError;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.completedAt = completedAt;
    }

    public static EnrollmentActivationTask create(
            UUID enrollmentId,
            UUID paymentId
    ) {
        Instant now = Instant.now();

        return new EnrollmentActivationTask(
                UUID.randomUUID(),
                enrollmentId,
                paymentId,
                EnrollmentActivationStatus.PENDING,
                0,
                null,
                now,
                now,
                null
        );
    }

    public void beginAttempt() {
        if (
                status
                        == EnrollmentActivationStatus.SUCCEEDED
        ) {
            return;
        }

        attemptCount++;
        status = EnrollmentActivationStatus.PENDING;
        lastError = null;
        updatedAt = Instant.now();
    }

    public void markSucceeded() {
        status = EnrollmentActivationStatus.SUCCEEDED;
        lastError = null;
        completedAt = Instant.now();
        updatedAt = completedAt;
    }

    public void markFailed(String errorMessage) {
        if (
                errorMessage == null
                        || errorMessage.isBlank()
        ) {
            lastError =
                    "Enrollment activation request failed";
        }
        else {
            String normalized = errorMessage.trim();

            lastError = normalized.length() <= 1000
                    ? normalized
                    : normalized.substring(0, 1000);
        }

        status = EnrollmentActivationStatus.FAILED;
        completedAt = null;
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getEnrollmentId() {
        return enrollmentId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public EnrollmentActivationStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public long getVersion() {
        return version;
    }
}