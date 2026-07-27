package com.centerflow.academic.courselevel.api;

import jakarta.validation.constraints.NotNull;

public record ChangeCourseLevelStatusRequest(

        @NotNull(message = "Course level active status is required")
        Boolean active
) {
}