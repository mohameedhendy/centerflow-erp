package com.centerflow.finance.pricing.repository;

import com.centerflow.finance.pricing.domain.PricingPlan;
import com.centerflow.finance.pricing.domain.PricingPlanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PricingPlanRepository
        extends JpaRepository<PricingPlan, UUID> {

    Optional<PricingPlan> findByCode(String code);

    boolean existsByCode(String code);

    Page<PricingPlan> findAllByStatus(
            PricingPlanStatus status,
            Pageable pageable
    );
}