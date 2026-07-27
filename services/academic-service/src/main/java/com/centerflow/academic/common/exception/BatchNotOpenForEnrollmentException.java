package com.centerflow.academic.common.exception;

import com.centerflow.academic.batch.domain.BatchStatus;

public class BatchNotOpenForEnrollmentException
        extends RuntimeException {

    public BatchNotOpenForEnrollmentException(
            BatchStatus status
    ) {
        super(
                "Seats can only be reserved while batch status is OPEN_FOR_ENROLLMENT. Current status: "
                        + status
        );
    }
}