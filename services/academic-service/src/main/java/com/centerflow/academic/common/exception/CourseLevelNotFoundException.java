package com.centerflow.academic.common.exception;

import java.util.UUID;

public class CourseLevelNotFoundException
        extends RuntimeException {

    public CourseLevelNotFoundException(UUID levelId) {
        super(
                "Course level was not found: "
                        + levelId
        );
    }
}