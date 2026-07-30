package com.centerflow.finance.refund.repository;

import com.centerflow.finance.refund.domain.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RefundRepository
        extends JpaRepository<Refund, UUID> {

    boolean existsByExternalReference(
            String externalReference
    );
}