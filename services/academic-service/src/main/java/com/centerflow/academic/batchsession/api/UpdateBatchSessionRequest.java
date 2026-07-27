package com.centerflow.academic.batchsession.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateBatchSessionRequest(

        @NotNull(message = "Session date is required")
        LocalDate sessionDate,

        @NotNull(message = "Session start time is required")
        LocalTime startTime,

        @NotNull(message = "Session end time is required")
        LocalTime endTime,

        @Size(
                max = 200,
                message = "Session topic must not exceed 200 characters"
        )
        String topic
) {
}