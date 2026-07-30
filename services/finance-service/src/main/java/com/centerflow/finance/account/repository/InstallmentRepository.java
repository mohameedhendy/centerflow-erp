package com.centerflow.finance.account.repository;

import com.centerflow.finance.account.domain.Installment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface InstallmentRepository
        extends JpaRepository<Installment, UUID> {

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
}