package com.centerflow.academic.common.exception;

import com.centerflow.academic.batchsession.domain.BatchSessionStatus;

public class InvalidSessionStatusTransitionException
        extends RuntimeException {

    public InvalidSessionStatusTransitionException(
            BatchSessionStatus currentStatus,
            BatchSessionStatus targetStatus
    ) {
        super(
                "Session status cannot change from "
                        + currentStatus
                        + " to "
                        + targetStatus
        );
    }
}