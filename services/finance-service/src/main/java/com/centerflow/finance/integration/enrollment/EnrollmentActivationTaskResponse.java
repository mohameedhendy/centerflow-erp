package com.centerflow.finance.integration.enrollment;

import java.time.Instant;
import java.util.UUID;

public record EnrollmentActivationTaskResponse(

        UUID id,
        UUID enrollmentId,
        UUID paymentId,
        EnrollmentActivationStatus status,
        int attemptCount,
        String lastError,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt

) {

    public static EnrollmentActivationTaskResponse from(
            EnrollmentActivationTask task
    ) {
        return new EnrollmentActivationTaskResponse(
                task.getId(),
                task.getEnrollmentId(),
                task.getPaymentId(),
                task.getStatus(),
                task.getAttemptCount(),
                task.getLastError(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getCompletedAt()
        );
    }
}