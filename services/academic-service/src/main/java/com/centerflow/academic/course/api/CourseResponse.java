package com.centerflow.academic.course.api;

import com.centerflow.academic.course.application.CourseResult;

import java.time.Instant;
import java.util.UUID;

public record CourseResponse(
        UUID id,
        String code,
        String name,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public static CourseResponse from(
            CourseResult result
    ) {
        return new CourseResponse(
                result.id(),
                result.code(),
                result.name(),
                result.description(),
                result.active(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}