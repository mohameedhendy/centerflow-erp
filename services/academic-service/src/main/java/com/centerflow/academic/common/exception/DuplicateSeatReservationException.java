package com.centerflow.academic.common.exception;

import java.util.UUID;

public class DuplicateSeatReservationException
        extends RuntimeException {

    public DuplicateSeatReservationException(
            UUID batchId,
            UUID enrollmentId,
            Throwable cause
    ) {
        super(
                "A seat reservation already exists for enrollment "
                        + enrollmentId
                        + " in batch "
                        + batchId,
                cause
        );
    }
}