package com.centerflow.academic.report.api;

public record BatchStatusSummaryResponse(

        long totalBatches,
        long draftBatches,
        long openForEnrollmentBatches,
        long inProgressBatches,
        long completedBatches,
        long cancelledBatches

) {
}