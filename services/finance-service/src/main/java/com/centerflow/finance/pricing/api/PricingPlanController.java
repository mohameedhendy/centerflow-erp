package com.centerflow.finance.pricing.api;

import com.centerflow.finance.common.api.PageResponse;
import com.centerflow.finance.pricing.api.dto.CreatePricingPlanRequest;
import com.centerflow.finance.pricing.api.dto.PricingPlanResponse;
import com.centerflow.finance.pricing.application.PricingPlanService;
import com.centerflow.finance.pricing.domain.PricingPlanStatus;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance/pricing-plans")
public class PricingPlanController {

    private final PricingPlanService pricingPlanService;

    public PricingPlanController(
            PricingPlanService pricingPlanService
    ) {
        this.pricingPlanService = pricingPlanService;
    }

    @PostMapping
    public ResponseEntity<PricingPlanResponse>
    createPricingPlan(
            @Valid @RequestBody
            CreatePricingPlanRequest request
    ) {
        PricingPlanResponse response =
                pricingPlanService.createPricingPlan(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<PricingPlanResponse>>
    searchPricingPlans(
            @RequestParam(required = false)
            String code,

            @RequestParam(required = false)
            String name,

            @RequestParam(required = false)
            PricingPlanStatus status,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        return ResponseEntity.ok(
                pricingPlanService.searchPricingPlans(
                        code,
                        name,
                        status,
                        page,
                        size
                )
        );
    }

    @GetMapping("/{pricingPlanId}")
    public ResponseEntity<PricingPlanResponse>
    getPricingPlan(
            @PathVariable UUID pricingPlanId
    ) {
        return ResponseEntity.ok(
                pricingPlanService.getPricingPlan(
                        pricingPlanId
                )
        );
    }

    @PostMapping("/{pricingPlanId}/activate")
    public ResponseEntity<PricingPlanResponse>
    activatePricingPlan(
            @PathVariable UUID pricingPlanId
    ) {
        return ResponseEntity.ok(
                pricingPlanService.activatePricingPlan(
                        pricingPlanId
                )
        );
    }

    @PostMapping("/{pricingPlanId}/deactivate")
    public ResponseEntity<PricingPlanResponse>
    deactivatePricingPlan(
            @PathVariable UUID pricingPlanId
    ) {
        return ResponseEntity.ok(
                pricingPlanService.deactivatePricingPlan(
                        pricingPlanId
                )
        );
    }
}