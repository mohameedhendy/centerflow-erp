package com.centerflow.academic.common.exception;

import com.centerflow.academic.batch.domain.BatchStatus;

public class BatchConfigurationLockedException
        extends RuntimeException {

    public BatchConfigurationLockedException(
            BatchStatus status
    ) {
        super(
                "Batch configuration cannot be changed while status is "
                        + status
        );
    }
}