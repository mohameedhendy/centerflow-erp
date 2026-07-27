package com.centerflow.academic.common.exception;

import java.util.UUID;

public class BatchScheduleNotFoundException
        extends RuntimeException {

    public BatchScheduleNotFoundException(
            UUID scheduleId
    ) {
        super(
                "Batch schedule was not found: "
                        + scheduleId
        );
    }
}