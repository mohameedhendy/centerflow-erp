package com.centerflow.academic.seatreservation.api;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReserveSeatRequest(

        @NotNull(message = "Enrollment ID is required")
        UUID enrollmentId
) {
}