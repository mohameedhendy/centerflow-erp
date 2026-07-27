package com.centerflow.academic.attendance.application;

import com.centerflow.academic.attendance.domain.AttendanceRecord;
import com.centerflow.academic.attendance.domain.AttendanceStatus;

import java.time.Instant;
import java.util.UUID;

public record AttendanceRecordResult(
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

    public static AttendanceRecordResult from(
            AttendanceRecord record
    ) {
        return new AttendanceRecordResult(
                record.getId(),
                record.getSessionId(),
                record.getEnrollmentId(),
                record.getStudentId(),
                record.getStatus(),
                record.getNotes(),
                record.getMarkedAt(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}