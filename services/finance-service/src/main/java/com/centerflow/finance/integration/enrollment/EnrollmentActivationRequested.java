package com.centerflow.finance.integration.enrollment;

import java.util.Objects;
import java.util.UUID;

public record EnrollmentActivationRequested(
        UUID taskId
) {

    public EnrollmentActivationRequested {
        Objects.requireNonNull(
                taskId,
                "Activation task ID is required"
        );
    }
}