package com.centerflow.finance.pricing.repository;

import com.centerflow.finance.pricing.domain.PricingPlan;
import com.centerflow.finance.pricing.domain.PricingPlanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            SELECT plan
            FROM PricingPlan plan
            WHERE (
                COALESCE(:code, '') = ''
                OR LOWER(plan.code)
                    LIKE LOWER(
                        CONCAT(
                            '%',
                            COALESCE(:code, ''),
                            '%'
                        )
                    )
            )
            AND (
                COALESCE(:name, '') = ''
                OR LOWER(plan.name)
                    LIKE LOWER(
                        CONCAT(
                            '%',
                            COALESCE(:name, ''),
                            '%'
                        )
                    )
            )
            AND (
                :status IS NULL
                OR plan.status = :status
            )
            """)
    Page<PricingPlan> search(
            @Param("code")
            String code,

            @Param("name")
            String name,

            @Param("status")
            PricingPlanStatus status,

            Pageable pageable
    );
}