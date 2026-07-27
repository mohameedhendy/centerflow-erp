package com.centerflow.academic.attendance.api;

import com.centerflow.academic.attendance.application.AttendanceSummaryResult;

import java.util.UUID;

public record AttendanceSummaryResponse(
        UUID sessionId,
        long total,
        long present,
        long absent,
        long late,
        long excused
) {

    public static AttendanceSummaryResponse from(
            AttendanceSummaryResult result
    ) {
        return new AttendanceSummaryResponse(
                result.sessionId(),
                result.total(),
                result.present(),
                result.absent(),
                result.late(),
                result.excused()
        );
    }
}