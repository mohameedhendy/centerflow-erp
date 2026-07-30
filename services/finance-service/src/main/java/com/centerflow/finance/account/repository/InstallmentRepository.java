package com.centerflow.finance.account.repository;

import com.centerflow.finance.account.domain.Installment;
import com.centerflow.finance.account.domain.InstallmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstallmentRepository
        extends JpaRepository<Installment, UUID>,
        JpaSpecificationExecutor<Installment> {

    List<Installment>
    findAllByFinancialAccountIdOrderByInstallmentNumberAsc(
            UUID financialAccountId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT installment
            FROM Installment installment
            WHERE installment.financialAccountId
                = :financialAccountId
            ORDER BY installment.installmentNumber ASC
            """)
    List<Installment>
    findAllByFinancialAccountIdForUpdate(
            @Param("financialAccountId")
            UUID financialAccountId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT installment
            FROM Installment installment
            WHERE installment.id = :installmentId
            """)
    Optional<Installment> findByIdForUpdate(
            @Param("installmentId")
            UUID installmentId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT installment
            FROM Installment installment
            WHERE installment.dueDate < :asOfDate
              AND installment.status IN :eligibleStatuses
            ORDER BY
                installment.dueDate ASC,
                installment.installmentNumber ASC
            """)
    List<Installment> findAllEligibleForOverdueUpdate(
            @Param("asOfDate")
            LocalDate asOfDate,

            @Param("eligibleStatuses")
            Collection<InstallmentStatus> eligibleStatuses
    );
}