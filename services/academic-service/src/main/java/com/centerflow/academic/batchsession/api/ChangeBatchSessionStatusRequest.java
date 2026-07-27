package com.centerflow.academic.batchsession.api;

import com.centerflow.academic.batchsession.domain.BatchSessionStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeBatchSessionStatusRequest(

        @NotNull(message = "Session status is required")
        BatchSessionStatus status
) {
}