package com.centerflow.academic.common.exception;

public class DuplicateBatchCodeException
        extends RuntimeException {

    public DuplicateBatchCodeException(String code) {
        super(
                "A batch already exists with code: "
                        + code
        );
    }

    public DuplicateBatchCodeException(
            String code,
            Throwable cause
    ) {
        super(
                "A batch already exists with code: "
                        + code,
                cause
        );
    }
}