package com.centerflow.academic.report.api;

public record SessionStatusSummaryResponse(

        long totalSessions,
        long plannedSessions,
        long completedSessions,
        long cancelledSessions

) {
}