package com.centerflow.academic.attendance.api;

import com.centerflow.academic.attendance.application.AttendanceRecordResult;
import com.centerflow.academic.attendance.domain.AttendanceStatus;

import java.time.Instant;
import java.util.UUID;

public record AttendanceRecordResponse(
        UUID id,
        UUID sessionId,
        UUID enrollmentId,
        UUID studentId,
        AttendanceStatus status,
        String notes,
        Instant markedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static AttendanceRecordResponse from(
            AttendanceRecordResult result
    ) {
        return new AttendanceRecordResponse(
                result.id(),
                result.sessionId(),
                result.enrollmentId(),
                result.studentId(),
                result.status(),
                result.notes(),
                result.markedAt(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}