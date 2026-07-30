package com.centerflow.finance.pricing.api;

import com.centerflow.finance.pricing.api.dto.PricingQuoteResponse;
import com.centerflow.finance.pricing.application.PricingQuoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(
        "/api/v1/finance/internal/pricing-quotes"
)
public class PricingQuoteController {

    private final PricingQuoteService pricingQuoteService;

    public PricingQuoteController(
            PricingQuoteService pricingQuoteService
    ) {
        this.pricingQuoteService = pricingQuoteService;
    }

    @GetMapping("/{pricingPlanId}")
    public ResponseEntity<PricingQuoteResponse>
    getPricingQuote(
            @PathVariable UUID pricingPlanId
    ) {
        return ResponseEntity.ok(
                pricingQuoteService.getPricingQuote(
                        pricingPlanId
                )
        );
    }
}