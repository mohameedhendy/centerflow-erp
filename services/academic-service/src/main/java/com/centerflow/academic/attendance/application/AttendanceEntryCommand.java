package com.centerflow.academic.attendance.application;

import com.centerflow.academic.attendance.domain.AttendanceStatus;

import java.util.UUID;

public record AttendanceEntryCommand(
        UUID enrollmentId,
        UUID studentId,
        AttendanceStatus status,
        String notes
) {
}