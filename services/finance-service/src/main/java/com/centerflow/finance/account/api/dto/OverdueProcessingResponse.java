package com.centerflow.finance.account.api.dto;

import java.time.LocalDate;

public record OverdueProcessingResponse(

        LocalDate asOfDate,
        int markedOverdueCount

) {
}