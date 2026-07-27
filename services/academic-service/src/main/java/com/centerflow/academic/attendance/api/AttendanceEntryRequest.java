package com.centerflow.academic.attendance.api;

import com.centerflow.academic.attendance.domain.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AttendanceEntryRequest(

        @NotNull(message = "Enrollment ID is required")
        UUID enrollmentId,

        @NotNull(message = "Student ID is required")
        UUID studentId,

        @NotNull(message = "Attendance status is required")
        AttendanceStatus status,

        @Size(
                max = 500,
                message = "Attendance notes must not exceed 500 characters"
        )
        String notes
) {
}