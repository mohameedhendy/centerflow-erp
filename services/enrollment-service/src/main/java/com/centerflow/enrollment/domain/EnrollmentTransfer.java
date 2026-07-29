package com.centerflow.enrollment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "enrollment_transfers")
public class EnrollmentTransfer {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(name = "from_batch_id", nullable = false)
    private UUID fromBatchId;

    @Column(name = "to_batch_id", nullable = false)
    private UUID toBatchId;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "transferred_at", nullable = false)
    private Instant transferredAt;

    protected EnrollmentTransfer() {
    }

    private EnrollmentTransfer(
            UUID id,
            UUID enrollmentId,
            UUID fromBatchId,
            UUID toBatchId,
            String reason,
            Instant transferredAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.enrollmentId = Objects.requireNonNull(enrollmentId);
        this.fromBatchId = Objects.requireNonNull(fromBatchId);
        this.toBatchId = Objects.requireNonNull(toBatchId);
        this.reason = normalizeReason(reason);
        this.transferredAt = Objects.requireNonNull(transferredAt);

        if (fromBatchId.equals(toBatchId)) {
            throw new IllegalArgumentException(
                    "Source and target batches must be different"
            );
        }
    }

    public static EnrollmentTransfer create(
            UUID enrollmentId,
            UUID fromBatchId,
            UUID toBatchId,
            String reason
    ) {
        return new EnrollmentTransfer(
                UUID.randomUUID(),
                enrollmentId,
                fromBatchId,
                toBatchId,
                reason,
                Instant.now()
        );
    }

    private static String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Transfer reason is required"
            );
        }

        String normalized = reason.trim();

        if (normalized.length() > 500) {
            throw new IllegalArgumentException(
                    "Transfer reason must not exceed 500 characters"
            );
        }

        return normalized;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEnrollmentId() {
        return enrollmentId;
    }

    public UUID getFromBatchId() {
        return fromBatchId;
    }

    public UUID getToBatchId() {
        return toBatchId;
    }

    public String getReason() {
        return reason;
    }

    public Instant getTransferredAt() {
        return transferredAt;
    }
}