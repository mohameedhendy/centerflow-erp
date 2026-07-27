package com.centerflow.academic.common.exception;

import java.util.UUID;

public class DuplicateClassroomCodeException
        extends RuntimeException {

    public DuplicateClassroomCodeException(
            UUID branchId,
            String code
    ) {
        super(
                "A classroom already exists with code "
                        + code
                        + " in branch "
                        + branchId
        );
    }

    public DuplicateClassroomCodeException(
            UUID branchId,
            String code,
            Throwable cause
    ) {
        super(
                "A classroom already exists with code "
                        + code
                        + " in branch "
                        + branchId,
                cause
        );
    }
}