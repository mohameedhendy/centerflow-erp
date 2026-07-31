package com.centerflow.academic.report.application;

import com.centerflow.academic.batch.repository.BatchRepository;
import com.centerflow.academic.branch.repository.BranchRepository;
import com.centerflow.academic.common.exception.BatchNotFoundException;
import com.centerflow.academic.common.exception.BranchNotFoundException;
import com.centerflow.academic.common.exception.InvalidAcademicReportPeriodException;
import com.centerflow.academic.report.api.AcademicOverviewResponse;
import com.centerflow.academic.report.api.BatchAcademicReportResponse;
import com.centerflow.academic.report.api.StudentAttendanceReportResponse;
import com.centerflow.academic.report.repository.AcademicReportQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class AcademicReportService {

    private final AcademicReportQueryRepository
            reportRepository;

    private final BranchRepository branchRepository;
    private final BatchRepository batchRepository;

    public AcademicReportService(
            AcademicReportQueryRepository reportRepository,
            BranchRepository branchRepository,
            BatchRepository batchRepository
    ) {
        this.reportRepository = reportRepository;
        this.branchRepository = branchRepository;
        this.batchRepository = batchRepository;
    }

    @Transactional(readOnly = true)
    public AcademicOverviewResponse getOverview(
            UUID branchId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        validatePeriod(fromDate, toDate);
        validateBranch(branchId);

        return reportRepository.findOverview(
                branchId,
                fromDate,
                toDate
        );
    }

    @Transactional(readOnly = true)
    public BatchAcademicReportResponse
    getBatchReport(
            UUID batchId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        validatePeriod(fromDate, toDate);

        if (!batchRepository.existsById(batchId)) {
            throw new BatchNotFoundException(batchId);
        }

        return reportRepository
                .findBatchReport(
                        batchId,
                        fromDate,
                        toDate
                )
                .orElseThrow(
                        () -> new BatchNotFoundException(
                                batchId
                        )
                );
    }

    @Transactional(readOnly = true)
    public StudentAttendanceReportResponse
    getStudentAttendanceReport(
            UUID studentId,
            UUID batchId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        validatePeriod(fromDate, toDate);

        if (
                batchId != null
                        && !batchRepository
                        .existsById(batchId)
        ) {
            throw new BatchNotFoundException(batchId);
        }

        return reportRepository
                .findStudentAttendanceReport(
                        studentId,
                        batchId,
                        fromDate,
                        toDate
                );
    }

    private void validateBranch(UUID branchId) {
        if (
                branchId != null
                        && !branchRepository
                        .existsById(branchId)
        ) {
            throw new BranchNotFoundException(
                    branchId
            );
        }
    }

    private void validatePeriod(
            LocalDate fromDate,
            LocalDate toDate
    ) {
        if (
                fromDate != null
                        && toDate != null
                        && fromDate.isAfter(toDate)
        ) {
            throw new InvalidAcademicReportPeriodException(
                    "Academic report from date "
                            + "must not be after to date"
            );
        }
    }
}