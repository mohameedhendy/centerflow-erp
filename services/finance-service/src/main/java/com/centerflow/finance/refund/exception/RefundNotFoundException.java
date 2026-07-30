package com.centerflow.finance.refund.exception;

import java.util.UUID;

public class RefundNotFoundException
        extends RuntimeException {

    public RefundNotFoundException(UUID refundId) {
        super("Refund not found: " + refundId);
    }
}