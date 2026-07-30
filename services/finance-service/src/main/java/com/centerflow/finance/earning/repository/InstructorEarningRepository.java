package com.centerflow.finance.earning.repository;

import com.centerflow.finance.earning.domain.InstructorEarning;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InstructorEarningRepository
        extends JpaRepository<InstructorEarning, UUID>,
        JpaSpecificationExecutor<InstructorEarning> {

    Optional<InstructorEarning> findBySessionId(
            UUID sessionId
    );

    boolean existsByPaymentReference(
            String paymentReference
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT earning
            FROM InstructorEarning earning
            WHERE earning.id = :earningId
            """)
    Optional<InstructorEarning> findByIdForUpdate(
            @Param("earningId")
            UUID earningId
    );
}