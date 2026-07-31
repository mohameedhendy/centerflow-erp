package com.centerflow.finance.report.application;

import com.centerflow.finance.report.api.FinancialAccountReportResponse;
import com.centerflow.finance.report.api.FinancialOverviewResponse;
import com.centerflow.finance.report.exception.FinancialReportAccountNotFoundException;
import com.centerflow.finance.report.exception.InvalidFinancialReportRequestException;
import com.centerflow.finance.report.repository.FinancialReportQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Service
public class FinancialReportService {

    private final FinancialReportQueryRepository
            reportRepository;

    public FinancialReportService(
            FinancialReportQueryRepository reportRepository
    ) {
        this.reportRepository = reportRepository;
    }

    @Transactional(readOnly = true)
    public FinancialOverviewResponse getOverview(
            String currency,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        String normalizedCurrency =
                normalizeCurrency(currency);

        validatePeriod(fromDate, toDate);

        return reportRepository.findOverview(
                normalizedCurrency,
                fromDate,
                toDate
        );
    }

    @Transactional(readOnly = true)
    public FinancialAccountReportResponse
    getAccountReport(
            UUID financialAccountId
    ) {
        return reportRepository
                .findAccountReport(
                        financialAccountId
                )
                .orElseThrow(
                        () ->
                                new FinancialReportAccountNotFoundException(
                                        financialAccountId
                                )
                );
    }

    private String normalizeCurrency(
            String currency
    ) {
        if (currency == null) {
            throw new InvalidFinancialReportRequestException(
                    "Financial report currency is required"
            );
        }

        String normalized =
                currency
                        .trim()
                        .toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z]{3}")) {
            throw new InvalidFinancialReportRequestException(
                    "Financial report currency "
                            + "must contain exactly three letters"
            );
        }

        return normalized;
    }

    private void validatePeriod(
            LocalDate fromDate,
            LocalDate toDate
    ) {
        if (fromDate == null || toDate == null) {
            throw new InvalidFinancialReportRequestException(
                    "Financial report from date "
                            + "and to date are required"
            );
        }

        if (fromDate.isAfter(toDate)) {
            throw new InvalidFinancialReportRequestException(
                    "Financial report from date "
                            + "must not be after to date"
            );
        }
    }
}