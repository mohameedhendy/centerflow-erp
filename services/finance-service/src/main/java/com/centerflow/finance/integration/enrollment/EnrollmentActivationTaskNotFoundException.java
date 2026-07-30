package com.centerflow.finance.integration.enrollment;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class EnrollmentActivationTaskNotFoundException
        extends RuntimeException {

    public EnrollmentActivationTaskNotFoundException(
            UUID taskId
    ) {
        super(
                "Enrollment activation task not found: "
                        + taskId
        );
    }

    public static EnrollmentActivationTaskNotFoundException
    forEnrollment(UUID enrollmentId) {
        return new EnrollmentActivationTaskNotFoundException(
                enrollmentId
        );
    }
}