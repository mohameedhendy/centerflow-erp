package com.centerflow.academic.batchschedule.api;

import jakarta.validation.constraints.NotNull;

public record ChangeBatchScheduleStatusRequest(

        @NotNull(message = "Schedule active status is required")
        Boolean active
) {
}