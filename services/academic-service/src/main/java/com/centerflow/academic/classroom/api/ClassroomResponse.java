package com.centerflow.academic.classroom.api;

import com.centerflow.academic.classroom.application.ClassroomResult;

import java.time.Instant;
import java.util.UUID;

public record ClassroomResponse(
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

    public static ClassroomResponse from(
            ClassroomResult result
    ) {
        return new ClassroomResponse(
                result.id(),
                result.branchId(),
                result.code(),
                result.name(),
                result.capacity(),
                result.floor(),
                result.active(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}