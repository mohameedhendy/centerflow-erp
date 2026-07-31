package com.centerflow.academic.report.api;

import com.centerflow.academic.batch.domain.BatchStatus;

import java.time.LocalDate;
import java.util.UUID;

public record BatchAcademicReportResponse(

        UUID batchId,
        String batchCode,
        String batchName,
        BatchStatus batchStatus,

        UUID branchId,
        String branchCode,
        String branchName,

        UUID courseId,
        String courseCode,
        String courseName,

        UUID courseLevelId,
        String courseLevelCode,
        String courseLevelName,

        UUID instructorId,
        String instructorName,

        int capacity,
        long currentlyReservedSeats,

        LocalDate batchStartDate,
        LocalDate batchEndDate,

        LocalDate fromDate,
        LocalDate toDate,

        SessionStatusSummaryResponse sessions,
        AttendanceSummaryResponse attendance

) {
}