package com.centerflow.academic.common.exception;

import com.centerflow.academic.batch.domain.BatchStatus;

public class InvalidBatchStatusTransitionException
        extends RuntimeException {

    public InvalidBatchStatusTransitionException(
            BatchStatus currentStatus,
            BatchStatus targetStatus
    ) {
        super(
                "Batch status cannot change from "
                        + currentStatus
                        + " to "
                        + targetStatus
        );
    }
}