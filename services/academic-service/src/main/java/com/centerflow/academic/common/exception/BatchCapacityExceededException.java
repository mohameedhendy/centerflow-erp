package com.centerflow.academic.common.exception;

import java.util.UUID;

public class BatchCapacityExceededException
        extends RuntimeException {

    public BatchCapacityExceededException(
            UUID batchId
    ) {
        super(
                "Batch has no available seats: "
                        + batchId
        );
    }
}