package com.centerflow.enrollment.exception;

import java.util.UUID;

public class EnrollmentNotFoundException
        extends RuntimeException {

    public EnrollmentNotFoundException(
            UUID enrollmentId
    ) {
        super(
                "Enrollment was not found with ID: "
                        + enrollmentId
        );
    }
}