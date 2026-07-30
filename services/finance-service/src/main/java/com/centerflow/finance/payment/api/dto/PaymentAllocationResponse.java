package com.centerflow.finance.payment.api.dto;

import com.centerflow.finance.payment.domain.PaymentAllocation;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentAllocationResponse(

        UUID installmentId,
        int allocationOrder,
        BigDecimal amount

) {

    public static PaymentAllocationResponse from(
            PaymentAllocation allocation
    ) {
        return new PaymentAllocationResponse(
                allocation.getInstallmentId(),
                allocation.getAllocationOrder(),
                allocation.getAmount()
        );
    }
}