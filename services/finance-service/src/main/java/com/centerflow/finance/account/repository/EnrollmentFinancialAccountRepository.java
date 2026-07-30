package com.centerflow.finance.account.repository;

import com.centerflow.finance.account.domain.EnrollmentFinancialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EnrollmentFinancialAccountRepository
        extends JpaRepository<
                EnrollmentFinancialAccount,
                UUID
                > {

    Optional<EnrollmentFinancialAccount>
    findByEnrollmentId(UUID enrollmentId);

    boolean existsByEnrollmentId(UUID enrollmentId);
}