package com.centerflow.finance.adjustment.api;

import com.centerflow.finance.adjustment.api.dto.FinancialAdjustmentResponse;
import com.centerflow.finance.adjustment.api.dto.RecordFinancialAdjustmentRequest;
import com.centerflow.finance.adjustment.application.FinancialAdjustmentService;
import com.centerflow.finance.adjustment.domain.FinancialAdjustmentType;
import com.centerflow.finance.common.api.PageResponse;
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
@RequestMapping("/api/v1/finance")
public class FinancialAdjustmentController {

    private final FinancialAdjustmentService
            adjustmentService;

    public FinancialAdjustmentController(
            FinancialAdjustmentService adjustmentService
    ) {
        this.adjustmentService = adjustmentService;
    }

    @PostMapping(
            "/enrollment-accounts/{enrollmentId}/adjustments"
    )
    public ResponseEntity<FinancialAdjustmentResponse>
    recordAdjustment(
            @PathVariable UUID enrollmentId,

            @Valid
            @RequestBody
            RecordFinancialAdjustmentRequest request
    ) {
        FinancialAdjustmentResponse response =
                adjustmentService.recordAdjustment(
                        enrollmentId,
                        request
                );

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path(
                        "/api/v1/finance/adjustments/{id}"
                )
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping(
            "/enrollment-accounts/{enrollmentId}/adjustments"
    )
    public ResponseEntity<
            PageResponse<FinancialAdjustmentResponse>
            > searchAdjustments(
            @PathVariable UUID enrollmentId,

            @RequestParam(required = false)
            FinancialAdjustmentType type,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        return ResponseEntity.ok(
                adjustmentService.searchAdjustments(
                        enrollmentId,
                        type,
                        page,
                        size
                )
        );
    }

    @GetMapping("/adjustments/{adjustmentId}")
    public ResponseEntity<FinancialAdjustmentResponse>
    getAdjustment(
            @PathVariable UUID adjustmentId
    ) {
        return ResponseEntity.ok(
                adjustmentService.getAdjustment(
                        adjustmentId
                )
        );
    }
}