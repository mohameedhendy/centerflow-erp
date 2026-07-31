package com.centerflow.academic.report.api;

import java.time.LocalDate;
import java.util.UUID;

public record AcademicOverviewResponse(

        UUID branchId,
        LocalDate fromDate,
        LocalDate toDate,
        BatchStatusSummaryResponse batches,
        SessionStatusSummaryResponse sessions,
        long currentlyReservedSeats,
        AttendanceSummaryResponse attendance

) {
}