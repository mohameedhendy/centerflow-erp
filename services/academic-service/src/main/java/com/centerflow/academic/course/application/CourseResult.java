package com.centerflow.academic.course.application;

import com.centerflow.academic.course.domain.Course;

import java.time.Instant;
import java.util.UUID;

public record CourseResult(
        UUID id,
        String code,
        String name,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public static CourseResult from(Course course) {
        return new CourseResult(
                course.getId(),
                course.getCode(),
                course.getName(),
                course.getDescription(),
                course.isActive(),
                course.getCreatedAt(),
                course.getUpdatedAt()
        );
    }
}