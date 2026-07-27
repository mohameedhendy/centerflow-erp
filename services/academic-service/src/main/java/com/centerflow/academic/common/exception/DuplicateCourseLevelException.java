package com.centerflow.academic.common.exception;

import java.util.UUID;

public class DuplicateCourseLevelException
        extends RuntimeException {

    public DuplicateCourseLevelException(
            UUID courseId,
            String code,
            int sequenceNumber
    ) {
        super(
                "A course level already exists with code "
                        + code
                        + " or sequence "
                        + sequenceNumber
                        + " in course "
                        + courseId
        );
    }

    public DuplicateCourseLevelException(
            UUID courseId,
            String code,
            int sequenceNumber,
            Throwable cause
    ) {
        super(
                "A course level already exists with code "
                        + code
                        + " or sequence "
                        + sequenceNumber
                        + " in course "
                        + courseId,
                cause
        );
    }
}