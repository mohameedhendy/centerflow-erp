package com.centerflow.enrollment.api;

import com.centerflow.enrollment.api.dto.EnrollmentTransferResponse;
import com.centerflow.enrollment.api.dto.PageResponse;
import com.centerflow.enrollment.api.dto.TransferEnrollmentRequest;
import com.centerflow.enrollment.application.EnrollmentTransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(
        "/api/v1/enrollments/{enrollmentId}/transfers"
)
public class EnrollmentTransferController {

    private final EnrollmentTransferService transferService;

    public EnrollmentTransferController(
            EnrollmentTransferService transferService
    ) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<EnrollmentTransferResponse>
    transferEnrollment(
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody
            TransferEnrollmentRequest request
    ) {
        return ResponseEntity.ok(
                transferService.transferEnrollment(
                        enrollmentId,
                        request
                )
        );
    }

    @GetMapping
    public ResponseEntity<
            PageResponse<EnrollmentTransferResponse>
            > getTransferHistory(
            @PathVariable UUID enrollmentId,
            @RequestParam(defaultValue = "0")
            int page,
            @RequestParam(defaultValue = "20")
            int size
    ) {
        return ResponseEntity.ok(
                transferService.getTransferHistory(
                        enrollmentId,
                        page,
                        size
                )
        );
    }
}