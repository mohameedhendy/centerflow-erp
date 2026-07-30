package com.centerflow.finance.adjustment.repository;

import com.centerflow.finance.adjustment.domain.FinancialAdjustment;
import com.centerflow.finance.adjustment.domain.FinancialAdjustmentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FinancialAdjustmentRepository
        extends JpaRepository<
                FinancialAdjustment,
                UUID
                > {

    boolean existsByExternalReference(
            String externalReference
    );

    Page<FinancialAdjustment>
    findAllByFinancialAccountId(
            UUID financialAccountId,
            Pageable pageable
    );

    Page<FinancialAdjustment>
    findAllByFinancialAccountIdAndType(
            UUID financialAccountId,
            FinancialAdjustmentType type,
            Pageable pageable
    );
}