package com.centerflow.academic.report.api;

import java.math.BigDecimal;

public record AttendanceSummaryResponse(

        long totalRecords,
        long present,
        long absent,
        long late,
        long excused,
        long rateEligibleRecords,
        BigDecimal attendanceRate

) {
}