package com.centerflow.finance.pricing.application;

import com.centerflow.finance.common.api.PageResponse;
import com.centerflow.finance.common.exception.InvalidPaginationException;
import com.centerflow.finance.pricing.api.dto.CreatePricingPlanRequest;
import com.centerflow.finance.pricing.api.dto.PricingPlanResponse;
import com.centerflow.finance.pricing.domain.PricingPlan;
import com.centerflow.finance.pricing.domain.PricingPlanStatus;
import com.centerflow.finance.pricing.exception.PricingPlanConflictException;
import com.centerflow.finance.pricing.exception.PricingPlanNotFoundException;
import com.centerflow.finance.pricing.repository.PricingPlanRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class PricingPlanService {

    private static final int MAX_PAGE_SIZE = 100;

    private final PricingPlanRepository pricingPlanRepository;

    public PricingPlanService(
            PricingPlanRepository pricingPlanRepository
    ) {
        this.pricingPlanRepository = pricingPlanRepository;
    }

    @Transactional
    public PricingPlanResponse createPricingPlan(
            CreatePricingPlanRequest request
    ) {
        String normalizedCode = normalizeCode(request.code());

        if (pricingPlanRepository.existsByCode(normalizedCode)) {
            throw new PricingPlanConflictException(
                    "Pricing plan code already exists: "
                            + normalizedCode
            );
        }

        PricingPlan pricingPlan = PricingPlan.create(
                normalizedCode,
                request.name(),
                request.description(),
                request.totalAmount(),
                request.currency(),
                request.installmentCount(),
                request.initialPaymentAmount()
        );

        PricingPlan savedPricingPlan =
                pricingPlanRepository.save(pricingPlan);

        return PricingPlanResponse.from(savedPricingPlan);
    }

    @Transactional(readOnly = true)
    public PricingPlanResponse getPricingPlan(
            UUID pricingPlanId
    ) {
        return PricingPlanResponse.from(
                getRequiredPricingPlan(pricingPlanId)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<PricingPlanResponse>
    searchPricingPlans(
            String code,
            String name,
            PricingPlanStatus status,
            int page,
            int size
    ) {
        validatePagination(page, size);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<PricingPlan> pricingPlans =
                pricingPlanRepository.search(
                        normalizeSearchText(code),
                        normalizeSearchText(name),
                        status,
                        pageRequest
                );

        return PageResponse.from(
                pricingPlans,
                PricingPlanResponse::from
        );
    }

    @Transactional
    public PricingPlanResponse activatePricingPlan(
            UUID pricingPlanId
    ) {
        PricingPlan pricingPlan =
                getRequiredPricingPlan(pricingPlanId);

        pricingPlan.activate();

        return PricingPlanResponse.from(pricingPlan);
    }

    @Transactional
    public PricingPlanResponse deactivatePricingPlan(
            UUID pricingPlanId
    ) {
        PricingPlan pricingPlan =
                getRequiredPricingPlan(pricingPlanId);

        pricingPlan.deactivate();

        return PricingPlanResponse.from(pricingPlan);
    }

    private PricingPlan getRequiredPricingPlan(
            UUID pricingPlanId
    ) {
        return pricingPlanRepository
                .findById(pricingPlanId)
                .orElseThrow(
                        () -> new PricingPlanNotFoundException(
                                pricingPlanId
                        )
                );
    }

    private String normalizeCode(String code) {
        return code
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private String normalizeSearchText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private void validatePagination(
            int page,
            int size
    ) {
        if (page < 0) {
            throw new InvalidPaginationException(
                    "Page index must be zero or greater"
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidPaginationException(
                    "Page size must be between 1 and "
                            + MAX_PAGE_SIZE
            );
        }
    }
}