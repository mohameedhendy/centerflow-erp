package com.centerflow.academic.seatreservation.domain;

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
@Table(name = "seat_reservations")
public class SeatReservation {

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
            name = "enrollment_id",
            nullable = false,
            updatable = false
    )
    private UUID enrollmentId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private SeatReservationStatus status;

    @Column(
            name = "reserved_at",
            nullable = false
    )
    private Instant reservedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

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

    protected SeatReservation() {
    }

    private SeatReservation(
            UUID batchId,
            UUID enrollmentId,
            Instant createdAt
    ) {
        this.batchId = Objects.requireNonNull(
                batchId,
                "Batch ID is required"
        );

        this.enrollmentId = Objects.requireNonNull(
                enrollmentId,
                "Enrollment ID is required"
        );

        Instant creationTime = Objects.requireNonNull(
                createdAt,
                "Creation time is required"
        );

        this.status = SeatReservationStatus.RESERVED;
        this.reservedAt = creationTime;
        this.releasedAt = null;
        this.createdAt = creationTime;
        this.updatedAt = creationTime;
    }

    public static SeatReservation create(
            UUID batchId,
            UUID enrollmentId,
            Instant createdAt
    ) {
        return new SeatReservation(
                batchId,
                enrollmentId,
                createdAt
        );
    }

    public void reserve(Instant reservedAt) {
        if (status == SeatReservationStatus.RESERVED) {
            return;
        }

        Instant reservationTime =
                Objects.requireNonNull(
                        reservedAt,
                        "Reservation time is required"
                );

        this.status = SeatReservationStatus.RESERVED;
        this.reservedAt = reservationTime;
        this.releasedAt = null;
        this.updatedAt = reservationTime;
    }

    public void release(Instant releasedAt) {
        if (status == SeatReservationStatus.RELEASED) {
            return;
        }

        Instant releaseTime =
                Objects.requireNonNull(
                        releasedAt,
                        "Release time is required"
                );

        this.status = SeatReservationStatus.RELEASED;
        this.releasedAt = releaseTime;
        this.updatedAt = releaseTime;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public UUID getEnrollmentId() {
        return enrollmentId;
    }

    public SeatReservationStatus getStatus() {
        return status;
    }

    public Instant getReservedAt() {
        return reservedAt;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isReserved() {
        return status == SeatReservationStatus.RESERVED;
    }
}