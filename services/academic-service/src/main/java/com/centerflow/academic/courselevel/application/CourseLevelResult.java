package com.centerflow.academic.courselevel.application;

import com.centerflow.academic.courselevel.domain.CourseLevel;

import java.time.Instant;
import java.util.UUID;

public record CourseLevelResult(
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

    public static CourseLevelResult from(
            CourseLevel level
    ) {
        return new CourseLevelResult(
                level.getId(),
                level.getCourseId(),
                level.getCode(),
                level.getName(),
                level.getSequenceNumber(),
                level.getDurationHours(),
                level.getDescription(),
                level.isActive(),
                level.getCreatedAt(),
                level.getUpdatedAt()
        );
    }
}