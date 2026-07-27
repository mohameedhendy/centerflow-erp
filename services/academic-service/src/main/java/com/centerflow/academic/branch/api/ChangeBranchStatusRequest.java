package com.centerflow.academic.branch.api;

import jakarta.validation.constraints.NotNull;

public record ChangeBranchStatusRequest(

        @NotNull(message = "Branch active status is required")
        Boolean active
) {
}