package com.centerflow.finance.refund.repository;

import com.centerflow.finance.refund.domain.RefundAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RefundAllocationRepository
        extends JpaRepository<RefundAllocation, UUID> {

    List<RefundAllocation>
    findAllByRefundIdOrderByAllocationOrderAsc(
            UUID refundId
    );
}