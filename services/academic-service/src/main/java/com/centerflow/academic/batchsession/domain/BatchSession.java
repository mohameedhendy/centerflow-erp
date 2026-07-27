package com.centerflow.academic.batchsession.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "batch_sessions")
public class BatchSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(
            name = "batch_id",
            nullable = false,
            updatable = false
    )
    private UUID batchId;

    @Column(
            name = "batch_schedule_id",
            updatable = false
    )
    private UUID batchScheduleId;

    @Column(
            name = "session_date",
            nullable = false
    )
    private LocalDate sessionDate;

    @Column(
            name = "start_time",
            nullable = false
    )
    private LocalTime startTime;

    @Column(
            name = "end_time",
            nullable = false
    )
    private LocalTime endTime;

    @Column(name = "topic", length = 200)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private BatchSessionStatus status;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BatchSession() {
    }

    private BatchSession(
            UUID batchId,
            UUID batchScheduleId,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            String topic,
            Instant createdAt
    ) {
        this.batchId = Objects.requireNonNull(
                batchId,
                "Batch ID is required"
        );

        this.batchScheduleId = batchScheduleId;

        configure(
                sessionDate,
                startTime,
                endTime,
                topic
        );

        this.status = BatchSessionStatus.PLANNED;

        this.createdAt = Objects.requireNonNull(
                createdAt,
                "Creation time is required"
        );

        this.updatedAt = createdAt;
    }

    public static BatchSession create(
            UUID batchId,
            UUID batchScheduleId,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            String topic,
            Instant createdAt
    ) {
        return new BatchSession(
                batchId,
                batchScheduleId,
                sessionDate,
                startTime,
                endTime,
                topic,
                createdAt
        );
    }

    public void updateDetails(
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            String topic,
            Instant updatedAt
    ) {
        configure(
                sessionDate,
                startTime,
                endTime,
                topic
        );

        this.updatedAt = Objects.requireNonNull(
                updatedAt,
                "Update time is required"
        );
    }

    public void changeStatus(
            BatchSessionStatus status,
            Instant updatedAt
    ) {
        if (this.status == status) {
            return;
        }

        this.status = Objects.requireNonNull(
                status,
                "Session status is required"
        );

        this.updatedAt = Objects.requireNonNull(
                updatedAt,
                "Update time is required"
        );
    }

    private void configure(
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            String topic
    ) {
        this.sessionDate = Objects.requireNonNull(
                sessionDate,
                "Session date is required"
        );

        this.startTime = Objects.requireNonNull(
                startTime,
                "Session start time is required"
        );

        this.endTime = Objects.requireNonNull(
                endTime,
                "Session end time is required"
        );

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(
                    "Session end time must be after start time"
            );
        }

        this.topic = normalizeOptionalText(topic, 200);
    }

    public UUID getId() {
        return id;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public UUID getBatchScheduleId() {
        return batchScheduleId;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getTopic() {
        return topic;
    }

    public BatchSessionStatus getStatus() {
        return status;
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