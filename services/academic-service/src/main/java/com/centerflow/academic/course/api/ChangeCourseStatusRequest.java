package com.centerflow.academic.course.api;

import jakarta.validation.constraints.NotNull;

public record ChangeCourseStatusRequest(

        @NotNull(message = "Course active status is required")
        Boolean active
) {
}