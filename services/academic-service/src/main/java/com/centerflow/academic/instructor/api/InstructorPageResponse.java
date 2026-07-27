package com.centerflow.academic.instructor.api;

import com.centerflow.academic.instructor.application.InstructorPageResult;

import java.util.List;

public record InstructorPageResponse(
        List<InstructorResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static InstructorPageResponse from(
            InstructorPageResult result
    ) {
        List<InstructorResponse> content =
                result.content()
                        .stream()
                        .map(InstructorResponse::from)
                        .toList();

        return new InstructorPageResponse(
                content,
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}