package com.centerflow.finance.refund.api.dto;

import com.centerflow.finance.refund.domain.RefundAllocation;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundAllocationResponse(

        UUID paymentAllocationId,
        UUID installmentId,
        int allocationOrder,
        BigDecimal amount

) {

    public static RefundAllocationResponse from(
            RefundAllocation allocation
    ) {
        return new RefundAllocationResponse(
                allocation.getPaymentAllocationId(),
                allocation.getInstallmentId(),
                allocation.getAllocationOrder(),
                allocation.getAmount()
        );
    }
}