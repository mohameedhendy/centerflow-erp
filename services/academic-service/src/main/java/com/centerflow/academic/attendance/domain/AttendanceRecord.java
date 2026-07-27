package com.centerflow.academic.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "attendance_records")
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(
            name = "session_id",
            nullable = false,
            updatable = false
    )
    private UUID sessionId;

    @Column(
            name = "enrollment_id",
            nullable = false,
            updatable = false
    )
    private UUID enrollmentId;

    @Column(
            name = "student_id",
            nullable = false,
            updatable = false
    )
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private AttendanceStatus status;

    @Column(
            name = "notes",
            length = 500
    )
    private String notes;

    @Column(
            name = "marked_at",
            nullable = false
    )
    private Instant markedAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    protected AttendanceRecord() {
    }

    private AttendanceRecord(
            UUID sessionId,
            UUID enrollmentId,
            UUID studentId,
            AttendanceStatus status,
            String notes,
            Instant markedAt
    ) {
        this.sessionId = Objects.requireNonNull(
                sessionId,
                "Session ID is required"
        );

        this.enrollmentId = Objects.requireNonNull(
                enrollmentId,
                "Enrollment ID is required"
        );

        this.studentId = Objects.requireNonNull(
                studentId,
                "Student ID is required"
        );

        Instant markingTime = Objects.requireNonNull(
                markedAt,
                "Attendance marking time is required"
        );

        this.status = Objects.requireNonNull(
                status,
                "Attendance status is required"
        );

        this.notes = normalizeOptionalText(
                notes,
                500
        );

        this.markedAt = markingTime;
        this.createdAt = markingTime;
        this.updatedAt = markingTime;
    }

    public static AttendanceRecord create(
            UUID sessionId,
            UUID enrollmentId,
            UUID studentId,
            AttendanceStatus status,
            String notes,
            Instant markedAt
    ) {
        return new AttendanceRecord(
                sessionId,
                enrollmentId,
                studentId,
                status,
                notes,
                markedAt
        );
    }

    public void mark(
            AttendanceStatus status,
            String notes,
            Instant markedAt
    ) {
        Instant markingTime = Objects.requireNonNull(
                markedAt,
                "Attendance marking time is required"
        );

        this.status = Objects.requireNonNull(
                status,
                "Attendance status is required"
        );

        this.notes = normalizeOptionalText(
                notes,
                500
        );

        this.markedAt = markingTime;
        this.updatedAt = markingTime;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getEnrollmentId() {
        return enrollmentId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getMarkedAt() {
        return markedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static String normalizeOptionalText(
            String value,
            int maximumLength
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalizedValue = value.strip();

        if (normalizedValue.length() > maximumLength) {
            throw new IllegalArgumentException(
                    "Value must not exceed "
                            + maximumLength
                            + " characters"
            );
        }

        return normalizedValue;
    }
}