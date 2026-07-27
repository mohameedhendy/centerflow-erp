package com.centerflow.academic.attendance.application;

import java.util.UUID;

public record AttendanceSummaryResult(
        UUID sessionId,
        long total,
        long present,
        long absent,
        long late,
        long excused
) {
}