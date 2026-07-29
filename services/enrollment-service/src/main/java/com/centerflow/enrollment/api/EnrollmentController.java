package com.centerflow.enrollment.api;

import com.centerflow.enrollment.api.dto.CreateEnrollmentRequest;
import com.centerflow.enrollment.api.dto.EnrollmentResponse;
import com.centerflow.enrollment.api.dto.PageResponse;
import com.centerflow.enrollment.application.EnrollmentService;
import com.centerflow.enrollment.domain.EnrollmentStatus;
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
@RequestMapping("/api/v1/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(
            EnrollmentService enrollmentService
    ) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping
    public ResponseEntity<EnrollmentResponse> createEnrollment(
            @Valid @RequestBody
            CreateEnrollmentRequest request
    ) {
        EnrollmentResponse response =
                enrollmentService.createEnrollment(request);

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
    public ResponseEntity<PageResponse<EnrollmentResponse>>
    searchEnrollments(
            @RequestParam(required = false)
            String enrollmentNumber,

            @RequestParam(required = false)
            UUID studentId,

            @RequestParam(required = false)
            UUID batchId,

            @RequestParam(required = false)
            EnrollmentStatus status,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        PageResponse<EnrollmentResponse> response =
                enrollmentService.searchEnrollments(
                        enrollmentNumber,
                        studentId,
                        batchId,
                        status,
                        page,
                        size
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{enrollmentId}")
    public ResponseEntity<EnrollmentResponse> getEnrollment(
            @PathVariable UUID enrollmentId
    ) {
        EnrollmentResponse response =
                enrollmentService.getEnrollment(enrollmentId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/internal/{enrollmentId}/activate")
    public ResponseEntity<EnrollmentResponse> activateEnrollment(
            @PathVariable UUID enrollmentId
    ) {
        EnrollmentResponse response =
                enrollmentService.activateEnrollment(enrollmentId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{enrollmentId}/suspend")
    public ResponseEntity<EnrollmentResponse> suspendEnrollment(
            @PathVariable UUID enrollmentId
    ) {
        EnrollmentResponse response =
                enrollmentService.suspendEnrollment(enrollmentId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{enrollmentId}/resume")
    public ResponseEntity<EnrollmentResponse> resumeEnrollment(
            @PathVariable UUID enrollmentId
    ) {
        EnrollmentResponse response =
                enrollmentService.resumeEnrollment(enrollmentId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{enrollmentId}/complete")
    public ResponseEntity<EnrollmentResponse> completeEnrollment(
            @PathVariable UUID enrollmentId
    ) {
        EnrollmentResponse response =
                enrollmentService.completeEnrollment(enrollmentId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{enrollmentId}/cancel")
    public ResponseEntity<EnrollmentResponse> cancelEnrollment(
            @PathVariable UUID enrollmentId
    ) {
        EnrollmentResponse response =
                enrollmentService.cancelEnrollment(enrollmentId);

        return ResponseEntity.ok(response);
    }
}