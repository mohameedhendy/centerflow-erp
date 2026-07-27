package com.centerflow.academic.common.exception;

public class DuplicateBranchCodeException
        extends RuntimeException {

    public DuplicateBranchCodeException(String code) {
        super(
                "A branch already exists with code: "
                        + code
        );
    }

    public DuplicateBranchCodeException(
            String code,
            Throwable cause
    ) {
        super(
                "A branch already exists with code: "
                        + code,
                cause
        );
    }
}