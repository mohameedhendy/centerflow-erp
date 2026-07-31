package com.centerflow.academic.report.api;

import java.time.LocalDate;
import java.util.UUID;

public record StudentAttendanceReportResponse(

        UUID studentId,
        UUID batchId,
        LocalDate fromDate,
        LocalDate toDate,
        long attendedBatches,
        long sessionsWithAttendance,
        AttendanceSummaryResponse attendance

) {
}