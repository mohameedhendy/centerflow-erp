package com.centerflow.academic.course.api;

import com.centerflow.academic.course.application.CoursePageResult;

import java.util.List;

public record CoursePageResponse(
        List<CourseResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static CoursePageResponse from(
            CoursePageResult result
    ) {
        List<CourseResponse> content =
                result.content()
                        .stream()
                        .map(CourseResponse::from)
                        .toList();

        return new CoursePageResponse(
                content,
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}