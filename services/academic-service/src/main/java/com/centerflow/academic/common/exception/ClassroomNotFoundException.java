package com.centerflow.academic.common.exception;

import java.util.UUID;

public class ClassroomNotFoundException
        extends RuntimeException {

    public ClassroomNotFoundException(UUID classroomId) {
        super(
                "Classroom was not found: "
                        + classroomId
        );
    }
}