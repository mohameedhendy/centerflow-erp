package com.centerflow.academic.batch.application;

import com.centerflow.academic.batch.domain.Batch;
import com.centerflow.academic.batch.domain.BatchStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BatchResult(
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

    public static BatchResult from(Batch batch) {
        return new BatchResult(
                batch.getId(),
                batch.getCode(),
                batch.getName(),
                batch.getBranchId(),
                batch.getClassroomId(),
                batch.getCourseLevelId(),
                batch.getInstructorId(),
                batch.getCapacity(),
                batch.getStartDate(),
                batch.getEndDate(),
                batch.getStatus(),
                batch.getCreatedAt(),
                batch.getUpdatedAt()
        );
    }
}