package com.centerflow.academic.batchschedule.api;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record CreateBatchScheduleRequest(

        @NotNull(message = "Schedule day is required")
        DayOfWeek dayOfWeek,

        @NotNull(message = "Schedule start time is required")
        LocalTime startTime,

        @NotNull(message = "Schedule end time is required")
        LocalTime endTime
) {
}