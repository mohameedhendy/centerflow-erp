package com.centerflow.academic.seatreservation.api;

import com.centerflow.academic.batch.domain.BatchStatus;
import com.centerflow.academic.seatreservation.application.SeatAvailabilityResult;

import java.util.UUID;

public record SeatAvailabilityResponse(
        UUID batchId,
        BatchStatus batchStatus,
        int capacity,
        long reservedSeats,
        long availableSeats,
        boolean full
) {

    public static SeatAvailabilityResponse from(
            SeatAvailabilityResult result
    ) {
        return new SeatAvailabilityResponse(
                result.batchId(),
                result.batchStatus(),
                result.capacity(),
                result.reservedSeats(),
                result.availableSeats(),
                result.full()
        );
    }
}