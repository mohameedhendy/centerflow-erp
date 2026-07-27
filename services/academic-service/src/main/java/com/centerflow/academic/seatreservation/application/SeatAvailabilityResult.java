package com.centerflow.academic.seatreservation.application;

import com.centerflow.academic.batch.domain.BatchStatus;

import java.util.UUID;

public record SeatAvailabilityResult(
        UUID batchId,
        BatchStatus batchStatus,
        int capacity,
        long reservedSeats,
        long availableSeats,
        boolean full
) {
}