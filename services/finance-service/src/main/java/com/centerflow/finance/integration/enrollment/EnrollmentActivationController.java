package com.centerflow.finance.integration.enrollment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(
        "/api/v1/finance/internal/enrollment-activation-tasks"
)
public class EnrollmentActivationController {

    private final EnrollmentActivationProcessor processor;

    public EnrollmentActivationController(
            EnrollmentActivationProcessor processor
    ) {
        this.processor = processor;
    }

    @GetMapping("/by-enrollment/{enrollmentId}")
    public ResponseEntity<
            EnrollmentActivationTaskResponse
            > getByEnrollment(
            @PathVariable UUID enrollmentId
    ) {
        return ResponseEntity.ok(
                processor.getByEnrollmentId(
                        enrollmentId
                )
        );
    }

    @PostMapping("/{taskId}/retry")
    public ResponseEntity<
            EnrollmentActivationTaskResponse
            > retry(
            @PathVariable UUID taskId
    ) {
        return ResponseEntity.ok(
                processor.process(taskId)
        );
    }
}