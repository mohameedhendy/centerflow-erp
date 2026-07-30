package com.centerflow.finance.adjustment.exception;

import java.util.UUID;

public class FinancialAdjustmentNotFoundException
        extends RuntimeException {

    public FinancialAdjustmentNotFoundException(
            UUID adjustmentId
    ) {
        super(
                "Financial adjustment not found: "
                        + adjustmentId
        );
    }
}