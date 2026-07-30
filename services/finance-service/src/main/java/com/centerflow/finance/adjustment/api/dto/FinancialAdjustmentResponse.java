package com.centerflow.finance.adjustment.api.dto;

import com.centerflow.finance.adjustment.domain.FinancialAdjustment;
import com.centerflow.finance.adjustment.domain.FinancialAdjustmentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FinancialAdjustmentResponse(

        UUID id,
        UUID financialAccountId,
        FinancialAdjustmentType type,
        BigDecimal amount,
        String currency,
        String reason,
        String externalReference,
        Instant createdAt

) {

    public static FinancialAdjustmentResponse from(
            FinancialAdjustment adjustment
    ) {
        return new FinancialAdjustmentResponse(
                adjustment.getId(),
                adjustment.getFinancialAccountId(),
                adjustment.getType(),
                adjustment.getAmount(),
                adjustment.getCurrency(),
                adjustment.getReason(),
                adjustment.getExternalReference(),
                adjustment.getCreatedAt()
        );
    }
}