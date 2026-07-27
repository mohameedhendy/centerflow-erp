package com.centerflow.academic.common.exception;

import java.util.UUID;

public class CourseNotFoundException
        extends RuntimeException {

    public CourseNotFoundException(UUID courseId) {
        super(
                "Course was not found: "
                        + courseId
        );
    }
}