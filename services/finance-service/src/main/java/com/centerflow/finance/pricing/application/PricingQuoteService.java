package com.centerflow.finance.pricing.application;

import com.centerflow.finance.pricing.api.dto.PricingQuoteResponse;
import com.centerflow.finance.pricing.domain.PricingPlan;
import com.centerflow.finance.pricing.domain.PricingPlanStatus;
import com.centerflow.finance.pricing.exception.PricingPlanConflictException;
import com.centerflow.finance.pricing.exception.PricingPlanNotFoundException;
import com.centerflow.finance.pricing.repository.PricingPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PricingQuoteService {

    private final PricingPlanRepository pricingPlanRepository;

    public PricingQuoteService(
            PricingPlanRepository pricingPlanRepository
    ) {
        this.pricingPlanRepository = pricingPlanRepository;
    }

    @Transactional(readOnly = true)
    public PricingQuoteResponse getPricingQuote(
            UUID pricingPlanId
    ) {
        PricingPlan pricingPlan = pricingPlanRepository
                .findById(pricingPlanId)
                .orElseThrow(
                        () -> new PricingPlanNotFoundException(
                                pricingPlanId
                        )
                );

        if (pricingPlan.getStatus()
                != PricingPlanStatus.ACTIVE) {

            throw new PricingPlanConflictException(
                    "Pricing plan is not active: "
                            + pricingPlanId
            );
        }

        return PricingQuoteResponse.from(pricingPlan);
    }
}