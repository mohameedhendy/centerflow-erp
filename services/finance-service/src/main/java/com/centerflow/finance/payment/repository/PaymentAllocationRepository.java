package com.centerflow.finance.payment.repository;

import com.centerflow.finance.payment.domain.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentAllocationRepository
        extends JpaRepository<PaymentAllocation, UUID> {

    List<PaymentAllocation>
    findAllByPaymentIdOrderByAllocationOrderAsc(
            UUID paymentId
    );
}