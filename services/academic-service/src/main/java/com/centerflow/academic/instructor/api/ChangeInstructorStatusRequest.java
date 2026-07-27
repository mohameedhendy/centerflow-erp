package com.centerflow.academic.instructor.api;

import jakarta.validation.constraints.NotNull;

public record ChangeInstructorStatusRequest(

        @NotNull(
                message = "Instructor active status is required"
        )
        Boolean active
) {
}