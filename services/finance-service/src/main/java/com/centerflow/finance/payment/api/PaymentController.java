package com.centerflow.finance.payment.api;

import com.centerflow.finance.payment.api.dto.PaymentResponse;
import com.centerflow.finance.payment.api.dto.RecordPaymentRequest;
import com.centerflow.finance.payment.application.PaymentService;
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
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(
            PaymentService paymentService
    ) {
        this.paymentService = paymentService;
    }

    @PostMapping(
            "/enrollment-accounts/{enrollmentId}/payments"
    )
    public ResponseEntity<PaymentResponse> recordPayment(
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody
            RecordPaymentRequest request
    ) {
        PaymentResponse response =
                paymentService.recordPayment(
                        enrollmentId,
                        request
                );

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path(
                        "/api/v1/finance/payments/{paymentId}"
                )
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable UUID paymentId
    ) {
        return ResponseEntity.ok(
                paymentService.getPayment(paymentId)
        );
    }
}