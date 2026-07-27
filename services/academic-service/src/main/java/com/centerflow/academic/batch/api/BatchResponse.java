package com.centerflow.academic.batch.api;

import com.centerflow.academic.batch.application.BatchResult;
import com.centerflow.academic.batch.domain.BatchStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BatchResponse(
        UUID id,
        String code,
        String name,
        UUID branchId,
        UUID classroomId,
        UUID courseLevelId,
        UUID instructorId,
        int capacity,
        LocalDate startDate,
        LocalDate endDate,
        BatchStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static BatchResponse from(
            BatchResult result
    ) {
        return new BatchResponse(
                result.id(),
                result.code(),
                result.name(),
                result.branchId(),
                result.classroomId(),
                result.courseLevelId(),
                result.instructorId(),
                result.capacity(),
                result.startDate(),
                result.endDate(),
                result.status(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}