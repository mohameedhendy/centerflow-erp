package com.centerflow.academic.courselevel.api;

import com.centerflow.academic.courselevel.application.CourseLevelResult;

import java.time.Instant;
import java.util.UUID;

public record CourseLevelResponse(
        UUID id,
        UUID courseId,
        String code,
        String name,
        int sequenceNumber,
        int durationHours,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public static CourseLevelResponse from(
            CourseLevelResult result
    ) {
        return new CourseLevelResponse(
                result.id(),
                result.courseId(),
                result.code(),
                result.name(),
                result.sequenceNumber(),
                result.durationHours(),
                result.description(),
                result.active(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}