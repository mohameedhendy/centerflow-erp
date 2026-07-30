package com.centerflow.finance.account.repository;

import com.centerflow.finance.account.domain.EnrollmentFinancialAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT account
            FROM EnrollmentFinancialAccount account
            WHERE account.enrollmentId = :enrollmentId
            """)
    Optional<EnrollmentFinancialAccount>
    findByEnrollmentIdForUpdate(
            @Param("enrollmentId")
            UUID enrollmentId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT account
            FROM EnrollmentFinancialAccount account
            WHERE account.id = :accountId
            """)
    Optional<EnrollmentFinancialAccount>
    findByIdForUpdate(
            @Param("accountId")
            UUID accountId
    );
}