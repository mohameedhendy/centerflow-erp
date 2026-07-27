package com.centerflow.academic.common.exception;

import java.util.UUID;

public class SeatReservationNotFoundException
        extends RuntimeException {

    public SeatReservationNotFoundException(
            UUID reservationId
    ) {
        super(
                "Seat reservation was not found: "
                        + reservationId
        );
    }
}