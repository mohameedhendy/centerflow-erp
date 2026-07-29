package com.centerflow.enrollment.api.dto;

import com.centerflow.enrollment.domain.Enrollment;
import com.centerflow.enrollment.domain.EnrollmentStatus;

import java.time.Instant;
import java.util.UUID;

public record EnrollmentResponse(

        UUID id,
        String enrollmentNumber,
        UUID studentId,
        UUID batchId,
        EnrollmentStatus status,
        Instant createdAt,
        Instant updatedAt

) {

    public static EnrollmentResponse from(
            Enrollment enrollment
    ) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getEnrollmentNumber(),
                enrollment.getStudentId(),
                enrollment.getBatchId(),
                enrollment.getStatus(),
                enrollment.getCreatedAt(),
                enrollment.getUpdatedAt()
        );
    }
}