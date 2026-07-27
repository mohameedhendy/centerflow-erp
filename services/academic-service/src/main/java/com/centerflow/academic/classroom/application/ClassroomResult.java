package com.centerflow.academic.classroom.application;

import com.centerflow.academic.classroom.domain.Classroom;

import java.time.Instant;
import java.util.UUID;

public record ClassroomResult(
        UUID id,
        UUID branchId,
        String code,
        String name,
        int capacity,
        String floor,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public static ClassroomResult from(
            Classroom classroom
    ) {
        return new ClassroomResult(
                classroom.getId(),
                classroom.getBranchId(),
                classroom.getCode(),
                classroom.getName(),
                classroom.getCapacity(),
                classroom.getFloor(),
                classroom.isActive(),
                classroom.getCreatedAt(),
                classroom.getUpdatedAt()
        );
    }
}