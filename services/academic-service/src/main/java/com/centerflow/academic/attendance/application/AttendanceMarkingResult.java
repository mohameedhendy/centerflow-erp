package com.centerflow.academic.attendance.application;

import java.util.List;

public record AttendanceMarkingResult(
        List<AttendanceRecordResult> records
) {

    public AttendanceMarkingResult {
        records = List.copyOf(records);
    }
}