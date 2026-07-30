package com.centerflow.finance.payment.repository;

import com.centerflow.finance.payment.domain.PaymentAllocation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PaymentAllocationRepository
        extends JpaRepository<PaymentAllocation, UUID> {

    List<PaymentAllocation>
    findAllByPaymentIdOrderByAllocationOrderAsc(
            UUID paymentId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT allocation
            FROM PaymentAllocation allocation
            WHERE allocation.paymentId = :paymentId
            ORDER BY allocation.allocationOrder DESC
            """)
    List<PaymentAllocation>
    findAllByPaymentIdForRefund(
            @Param("paymentId")
            UUID paymentId
    );
}