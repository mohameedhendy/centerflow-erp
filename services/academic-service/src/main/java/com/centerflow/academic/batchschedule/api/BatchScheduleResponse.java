package com.centerflow.academic.batchschedule.api;

import com.centerflow.academic.batchschedule.application.BatchScheduleResult;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record BatchScheduleResponse(
        UUID id,
        UUID batchId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public static BatchScheduleResponse from(
            BatchScheduleResult result
    ) {
        return new BatchScheduleResponse(
                result.id(),
                result.batchId(),
                result.dayOfWeek(),
                result.startTime(),
                result.endTime(),
                result.active(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}