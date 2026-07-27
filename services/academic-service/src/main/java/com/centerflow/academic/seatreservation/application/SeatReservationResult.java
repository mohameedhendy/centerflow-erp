package com.centerflow.academic.seatreservation.application;

import com.centerflow.academic.seatreservation.domain.SeatReservation;
import com.centerflow.academic.seatreservation.domain.SeatReservationStatus;

import java.time.Instant;
import java.util.UUID;

public record SeatReservationResult(
        UUID id,
        UUID batchId,
        UUID enrollmentId,
        SeatReservationStatus status,
        Instant reservedAt,
        Instant releasedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static SeatReservationResult from(
            SeatReservation reservation
    ) {
        return new SeatReservationResult(
                reservation.getId(),
                reservation.getBatchId(),
                reservation.getEnrollmentId(),
                reservation.getStatus(),
                reservation.getReservedAt(),
                reservation.getReleasedAt(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }
}