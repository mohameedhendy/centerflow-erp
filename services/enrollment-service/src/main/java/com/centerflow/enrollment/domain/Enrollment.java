package com.centerflow.enrollment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "enrollments")
public class Enrollment {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(
            name = "enrollment_number",
            nullable = false,
            unique = true,
            length = 30
    )
    private String enrollmentNumber;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EnrollmentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Enrollment() {
    }

    private Enrollment(
            UUID id,
            String enrollmentNumber,
            UUID studentId,
            UUID batchId,
            EnrollmentStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.enrollmentNumber =
                normalizeEnrollmentNumber(enrollmentNumber);
        this.studentId = Objects.requireNonNull(studentId);
        this.batchId = Objects.requireNonNull(batchId);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static Enrollment create(
            String enrollmentNumber,
            UUID studentId,
            UUID batchId
    ) {
        Instant now = Instant.now();

        return new Enrollment(
                UUID.randomUUID(),
                enrollmentNumber,
                studentId,
                batchId,
                EnrollmentStatus.PENDING_PAYMENT,
                now,
                now
        );
    }

    public void activate() {
        if (status != EnrollmentStatus.PENDING_PAYMENT) {
            throw invalidTransition(EnrollmentStatus.ACTIVE);
        }

        changeStatus(EnrollmentStatus.ACTIVE);
    }

    public void suspend() {
        if (status != EnrollmentStatus.ACTIVE) {
            throw invalidTransition(EnrollmentStatus.SUSPENDED);
        }

        changeStatus(EnrollmentStatus.SUSPENDED);
    }

    public void resume() {
        if (status != EnrollmentStatus.SUSPENDED) {
            throw invalidTransition(EnrollmentStatus.ACTIVE);
        }

        changeStatus(EnrollmentStatus.ACTIVE);
    }

    public void complete() {
        if (status != EnrollmentStatus.ACTIVE) {
            throw invalidTransition(EnrollmentStatus.COMPLETED);
        }

        changeStatus(EnrollmentStatus.COMPLETED);
    }

    public void cancel() {
        if (status == EnrollmentStatus.COMPLETED) {
            throw invalidTransition(EnrollmentStatus.CANCELLED);
        }

        if (status == EnrollmentStatus.CANCELLED) {
            return;
        }

        changeStatus(EnrollmentStatus.CANCELLED);
    }

    private void changeStatus(
            EnrollmentStatus newStatus
    ) {
        status = newStatus;
        updatedAt = Instant.now();
    }

    private InvalidEnrollmentStatusTransitionException
    invalidTransition(
            EnrollmentStatus targetStatus
    ) {
        return new InvalidEnrollmentStatusTransitionException(
                status,
                targetStatus
        );
    }

    private static String normalizeEnrollmentNumber(
            String enrollmentNumber
    ) {
        if (
                enrollmentNumber == null
                        || enrollmentNumber.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Enrollment number is required"
            );
        }

        String normalized = enrollmentNumber
                .trim()
                .toUpperCase(Locale.ROOT);

        if (normalized.length() > 30) {
            throw new IllegalArgumentException(
                    "Enrollment number must not exceed 30 characters"
            );
        }

        return normalized;
    }

    public UUID getId() {
        return id;
    }

    public String getEnrollmentNumber() {
        return enrollmentNumber;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public EnrollmentStatus getStatus() {
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