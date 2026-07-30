package com.centerflow.finance.refund.api;

import com.centerflow.finance.refund.api.dto.RecordRefundRequest;
import com.centerflow.finance.refund.api.dto.RefundResponse;
import com.centerflow.finance.refund.application.RefundService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance")
public class RefundController {

    private final RefundService refundService;

    public RefundController(
            RefundService refundService
    ) {
        this.refundService = refundService;
    }

    @PostMapping("/payments/{paymentId}/refunds")
    public ResponseEntity<RefundResponse> recordRefund(
            @PathVariable UUID paymentId,
            @Valid @RequestBody
            RecordRefundRequest request
    ) {
        RefundResponse response =
                refundService.recordRefund(
                        paymentId,
                        request
                );

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path(
                        "/api/v1/finance/refunds/{refundId}"
                )
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping("/refunds/{refundId}")
    public ResponseEntity<RefundResponse> getRefund(
            @PathVariable UUID refundId
    ) {
        return ResponseEntity.ok(
                refundService.getRefund(refundId)
        );
    }
}