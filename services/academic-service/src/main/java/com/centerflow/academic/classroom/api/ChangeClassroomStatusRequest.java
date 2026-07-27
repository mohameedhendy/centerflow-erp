package com.centerflow.academic.classroom.api;

import jakarta.validation.constraints.NotNull;

public record ChangeClassroomStatusRequest(

        @NotNull(
                message = "Classroom active status is required"
        )
        Boolean active
) {
}