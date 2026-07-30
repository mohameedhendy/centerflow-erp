package com.centerflow.finance.account.repository;

import com.centerflow.finance.account.domain.Installment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InstallmentRepository
        extends JpaRepository<Installment, UUID> {

    List<Installment>
    findAllByFinancialAccountIdOrderByInstallmentNumberAsc(
            UUID financialAccountId
    );
}