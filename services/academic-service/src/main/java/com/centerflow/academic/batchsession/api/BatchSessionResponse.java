package com.centerflow.academic.batchsession.api;

import com.centerflow.academic.batchsession.application.BatchSessionResult;
import com.centerflow.academic.batchsession.domain.BatchSessionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BatchSessionResponse(
        UUID id,
        UUID batchId,
        UUID batchScheduleId,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        String topic,
        BatchSessionStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static BatchSessionResponse from(
            BatchSessionResult result
    ) {
        return new BatchSessionResponse(
                result.id(),
                result.batchId(),
                result.batchScheduleId(),
                result.sessionDate(),
                result.startTime(),
                result.endTime(),
                result.topic(),
                result.status(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}