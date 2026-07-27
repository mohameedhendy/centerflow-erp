package com.centerflow.academic.common.exception;

import java.util.UUID;

public class BranchNotFoundException
        extends RuntimeException {

    public BranchNotFoundException(UUID branchId) {
        super("Branch was not found: " + branchId);
    }
}