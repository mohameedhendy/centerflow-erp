package com.centerflow.finance.payment.repository;

import com.centerflow.finance.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRepository
        extends JpaRepository<Payment, UUID> {

    boolean existsByExternalReference(
            String externalReference
    );
}