package com.centerflow.enrollment.api.dto;

import com.centerflow.enrollment.domain.EnrollmentTransfer;

import java.time.Instant;
import java.util.UUID;

public record EnrollmentTransferResponse(

        UUID id,
        UUID enrollmentId,
        UUID fromBatchId,
        UUID toBatchId,
        String reason,
        Instant transferredAt

) {

    public static EnrollmentTransferResponse from(
            EnrollmentTransfer transfer
    ) {
        return new EnrollmentTransferResponse(
                transfer.getId(),
                transfer.getEnrollmentId(),
                transfer.getFromBatchId(),
                transfer.getToBatchId(),
                transfer.getReason(),
                transfer.getTransferredAt()
        );
    }
}