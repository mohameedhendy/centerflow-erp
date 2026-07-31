package com.centerflow.finance.report.api;

import com.centerflow.finance.report.application.FinancialReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance/reports")
public class FinancialReportController {

    private final FinancialReportService reportService;

    public FinancialReportController(
            FinancialReportService reportService
    ) {
        this.reportService = reportService;
    }

    @GetMapping("/overview")
    public FinancialOverviewResponse getOverview(
            @RequestParam String currency,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate fromDate,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate toDate
    ) {
        return reportService.getOverview(
                currency,
                fromDate,
                toDate
        );
    }

    @GetMapping("/accounts/{financialAccountId}")
    public FinancialAccountReportResponse
    getAccountReport(
            @PathVariable UUID financialAccountId
    ) {
        return reportService.getAccountReport(
                financialAccountId
        );
    }
}