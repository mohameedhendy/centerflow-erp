package com.centerflow.academic.seatreservation.api;

import com.centerflow.academic.seatreservation.application.SeatReservationResult;
import com.centerflow.academic.seatreservation.domain.SeatReservationStatus;

import java.time.Instant;
import java.util.UUID;

public record SeatReservationResponse(
        UUID id,
        UUID batchId,
        UUID enrollmentId,
        SeatReservationStatus status,
        Instant reservedAt,
        Instant releasedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static SeatReservationResponse from(
            SeatReservationResult result
    ) {
        return new SeatReservationResponse(
                result.id(),
                result.batchId(),
                result.enrollmentId(),
                result.status(),
                result.reservedAt(),
                result.releasedAt(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}