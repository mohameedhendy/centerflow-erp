package com.centerflow.finance.payment.repository;

import com.centerflow.finance.payment.domain.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository
        extends JpaRepository<Payment, UUID> {

    boolean existsByExternalReference(
            String externalReference
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT payment
            FROM Payment payment
            WHERE payment.id = :paymentId
            """)
    Optional<Payment> findByIdForUpdate(
            @Param("paymentId")
            UUID paymentId
    );
}