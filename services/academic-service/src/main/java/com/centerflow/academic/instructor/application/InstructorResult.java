package com.centerflow.academic.instructor.application;

import com.centerflow.academic.instructor.domain.Instructor;

import java.time.Instant;
import java.util.UUID;

public record InstructorResult(
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

    public static InstructorResult from(
            Instructor instructor
    ) {
        return new InstructorResult(
                instructor.getId(),
                instructor.getCode(),
                instructor.getFirstName(),
                instructor.getLastName(),
                instructor.getEmail(),
                instructor.getPhone(),
                instructor.getSpecialization(),
                instructor.getBio(),
                instructor.isActive(),
                instructor.getCreatedAt(),
                instructor.getUpdatedAt()
        );
    }
}