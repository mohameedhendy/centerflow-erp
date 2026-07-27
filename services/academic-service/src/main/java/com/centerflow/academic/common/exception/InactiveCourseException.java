package com.centerflow.academic.common.exception;

import java.util.UUID;

public class InactiveCourseException
        extends RuntimeException {

    public InactiveCourseException(UUID courseId) {
        super(
                "Course is inactive: "
                        + courseId
        );
    }
}