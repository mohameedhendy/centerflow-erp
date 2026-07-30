package com.centerflow.finance.account.api;

import com.centerflow.finance.account.api.dto.CreateEnrollmentFinancialAccountRequest;
import com.centerflow.finance.account.api.dto.EnrollmentFinancialAccountResponse;
import com.centerflow.finance.account.application.EnrollmentFinancialAccountService;
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
public class EnrollmentFinancialAccountController {

    private final EnrollmentFinancialAccountService
            financialAccountService;

    public EnrollmentFinancialAccountController(
            EnrollmentFinancialAccountService
                    financialAccountService
    ) {
        this.financialAccountService =
                financialAccountService;
    }

    @PostMapping("/internal/enrollment-accounts")
    public ResponseEntity<EnrollmentFinancialAccountResponse>
    createFinancialAccount(
            @Valid @RequestBody
            CreateEnrollmentFinancialAccountRequest request
    ) {
        EnrollmentFinancialAccountResponse response =
                financialAccountService
                        .createFinancialAccount(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path(
                        "/api/v1/finance/"
                                + "enrollment-accounts/"
                                + "{enrollmentId}"
                )
                .buildAndExpand(response.enrollmentId())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping(
            "/enrollment-accounts/{enrollmentId}"
    )
    public ResponseEntity<EnrollmentFinancialAccountResponse>
    getFinancialAccount(
            @PathVariable UUID enrollmentId
    ) {
        return ResponseEntity.ok(
                financialAccountService
                        .getFinancialAccount(enrollmentId)
        );
    }
}