package com.centerflow.academic.batch.api;

import com.centerflow.academic.batch.domain.BatchStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeBatchStatusRequest(

        @NotNull(message = "Batch status is required")
        BatchStatus status
) {
}