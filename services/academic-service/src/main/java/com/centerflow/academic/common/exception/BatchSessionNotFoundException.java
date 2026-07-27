package com.centerflow.academic.common.exception;

import java.util.UUID;

public class BatchSessionNotFoundException
        extends RuntimeException {

    public BatchSessionNotFoundException(
            UUID sessionId
    ) {
        super(
                "Batch session was not found: "
                        + sessionId
        );
    }
}