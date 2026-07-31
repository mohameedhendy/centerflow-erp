package com.centerflow.academic.report.api;

import com.centerflow.academic.report.application.AcademicReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/academic/reports")
public class AcademicReportController {

    private final AcademicReportService reportService;

    public AcademicReportController(
            AcademicReportService reportService
    ) {
        this.reportService = reportService;
    }

    @GetMapping("/overview")
    public AcademicOverviewResponse getOverview(
            @RequestParam(required = false)
            UUID branchId,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate toDate
    ) {
        return reportService.getOverview(
                branchId,
                fromDate,
                toDate
        );
    }

    @GetMapping("/batches/{batchId}")
    public BatchAcademicReportResponse
    getBatchReport(
            @PathVariable UUID batchId,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate toDate
    ) {
        return reportService.getBatchReport(
                batchId,
                fromDate,
                toDate
        );
    }

    @GetMapping(
            "/students/{studentId}/attendance"
    )
    public StudentAttendanceReportResponse
    getStudentAttendanceReport(
            @PathVariable UUID studentId,

            @RequestParam(required = false)
            UUID batchId,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate toDate
    ) {
        return reportService
                .getStudentAttendanceReport(
                        studentId,
                        batchId,
                        fromDate,
                        toDate
                );
    }
}