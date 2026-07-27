package com.centerflow.academic.batchschedule.application;

import com.centerflow.academic.batchschedule.domain.BatchSchedule;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record BatchScheduleResult(
        UUID id,
        UUID batchId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public static BatchScheduleResult from(
            BatchSchedule schedule
    ) {
        return new BatchScheduleResult(
                schedule.getId(),
                schedule.getBatchId(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.isActive(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}