package com.centerflow.enrollment.domain;

public class InvalidEnrollmentStatusTransitionException
        extends IllegalStateException {

    public InvalidEnrollmentStatusTransitionException(
            EnrollmentStatus currentStatus,
            EnrollmentStatus targetStatus
    ) {
        super(
                "Enrollment status cannot change from "
                        + currentStatus
                        + " to "
                        + targetStatus
        );
    }
}