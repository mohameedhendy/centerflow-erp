package com.centerflow.academic.batchsession.application;

import com.centerflow.academic.batchsession.domain.BatchSession;
import com.centerflow.academic.batchsession.domain.BatchSessionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BatchSessionResult(
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

    public static BatchSessionResult from(
            BatchSession session
    ) {
        return new BatchSessionResult(
                session.getId(),
                session.getBatchId(),
                session.getBatchScheduleId(),
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getTopic(),
                session.getStatus(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}