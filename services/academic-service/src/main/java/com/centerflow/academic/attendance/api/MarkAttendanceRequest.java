package com.centerflow.academic.attendance.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MarkAttendanceRequest(

        @NotEmpty(
                message = "At least one attendance record is required"
        )
        @Size(
                max = 500,
                message = "Attendance request must not contain more than 500 records"
        )
        List<@Valid AttendanceEntryRequest> records
) {
}