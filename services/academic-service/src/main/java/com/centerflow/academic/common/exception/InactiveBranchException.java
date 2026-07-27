package com.centerflow.academic.common.exception;

import java.util.UUID;

public class InactiveBranchException
        extends RuntimeException {

    public InactiveBranchException(UUID branchId) {
        super(
                "Branch is inactive: "
                        + branchId
        );
    }
}