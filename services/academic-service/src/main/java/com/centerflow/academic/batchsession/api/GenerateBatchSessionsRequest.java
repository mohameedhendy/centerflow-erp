package com.centerflow.academic.batchsession.api;

import java.time.LocalDate;

public record GenerateBatchSessionsRequest(
        LocalDate dateFrom,
        LocalDate dateTo
) {
}