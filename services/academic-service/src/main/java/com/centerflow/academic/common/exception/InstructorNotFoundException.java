package com.centerflow.academic.common.exception;

import java.util.UUID;

public class InstructorNotFoundException
        extends RuntimeException {

    public InstructorNotFoundException(
            UUID instructorId
    ) {
        super(
                "Instructor was not found: "
                        + instructorId
        );
    }
}