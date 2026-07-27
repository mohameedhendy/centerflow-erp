package com.centerflow.academic.common.exception;

public class DuplicateCourseCodeException
        extends RuntimeException {

    public DuplicateCourseCodeException(String code) {
        super(
                "A course already exists with code: "
                        + code
        );
    }

    public DuplicateCourseCodeException(
            String code,
            Throwable cause
    ) {
        super(
                "A course already exists with code: "
                        + code,
                cause
        );
    }
}