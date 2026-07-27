package com.centerflow.academic.instructor.api;

import com.centerflow.academic.instructor.application.InstructorResult;

import java.time.Instant;
import java.util.UUID;

public record InstructorResponse(
        UUID id,
        String code,
        String firstName,
        String lastName,
        String email,
        String phone,
        String specialization,
        String bio,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public static InstructorResponse from(
            InstructorResult result
    ) {
        return new InstructorResponse(
                result.id(),
                result.code(),
                result.firstName(),
                result.lastName(),
                result.email(),
                result.phone(),
                result.specialization(),
                result.bio(),
                result.active(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}