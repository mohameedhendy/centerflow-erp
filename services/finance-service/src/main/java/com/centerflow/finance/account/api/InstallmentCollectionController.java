package com.centerflow.finance.account.api;

import com.centerflow.finance.account.api.dto.InstallmentCollectionResponse;
import com.centerflow.finance.account.api.dto.OverdueProcessingResponse;
import com.centerflow.finance.account.application.InstallmentCollectionService;
import com.centerflow.finance.account.domain.InstallmentStatus;
import com.centerflow.finance.common.api.PageResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance")
public class InstallmentCollectionController {

    private final InstallmentCollectionService
            installmentCollectionService;

    public InstallmentCollectionController(
            InstallmentCollectionService
                    installmentCollectionService
    ) {
        this.installmentCollectionService =
                installmentCollectionService;
    }

    @PostMapping(
            "/internal/installments/mark-overdue"
    )
    public ResponseEntity<OverdueProcessingResponse>
    markOverdueInstallments(
            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate asOfDate
    ) {
        return ResponseEntity.ok(
                installmentCollectionService
                        .markOverdueInstallments(
                                asOfDate
                        )
        );
    }

    @GetMapping("/installments")
    public ResponseEntity<
            PageResponse<InstallmentCollectionResponse>
            > searchInstallments(
            @RequestParam(required = false)
            UUID enrollmentId,

            @RequestParam(required = false)
            UUID studentId,

            @RequestParam(required = false)
            InstallmentStatus status,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate dueFrom,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate dueTo,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        return ResponseEntity.ok(
                installmentCollectionService
                        .searchInstallments(
                                enrollmentId,
                                studentId,
                                status,
                                dueFrom,
                                dueTo,
                                page,
                                size
                        )
        );
    }
}