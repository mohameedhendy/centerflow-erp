package com.centerflow.academic.batchschedule.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "batch_schedules")
public class BatchSchedule {

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

    @Enumerated(EnumType.STRING)
    @Column(
            name = "day_of_week",
            nullable = false,
            length = 15
    )
    private DayOfWeek dayOfWeek;

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

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BatchSchedule() {
    }

    private BatchSchedule(
            UUID batchId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Instant createdAt
    ) {
        this.batchId = Objects.requireNonNull(
                batchId,
                "Batch ID is required"
        );

        configure(
                dayOfWeek,
                startTime,
                endTime
        );

        this.active = true;

        this.createdAt = Objects.requireNonNull(
                createdAt,
                "Creation time is required"
        );

        this.updatedAt = createdAt;
    }

    public static BatchSchedule create(
            UUID batchId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Instant createdAt
    ) {
        return new BatchSchedule(
                batchId,
                dayOfWeek,
                startTime,
                endTime,
                createdAt
        );
    }

    public void update(
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Instant updatedAt
    ) {
        configure(
                dayOfWeek,
                startTime,
                endTime
        );

        this.updatedAt = Objects.requireNonNull(
                updatedAt,
                "Update time is required"
        );
    }

    public void changeStatus(
            boolean active,
            Instant updatedAt
    ) {
        if (this.active == active) {
            return;
        }

        this.active = active;

        this.updatedAt = Objects.requireNonNull(
                updatedAt,
                "Update time is required"
        );
    }

    private void configure(
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {
        this.dayOfWeek = Objects.requireNonNull(
                dayOfWeek,
                "Schedule day is required"
        );

        this.startTime = Objects.requireNonNull(
                startTime,
                "Schedule start time is required"
        );

        this.endTime = Objects.requireNonNull(
                endTime,
                "Schedule end time is required"
        );

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(
                    "Schedule end time must be after start time"
            );
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}