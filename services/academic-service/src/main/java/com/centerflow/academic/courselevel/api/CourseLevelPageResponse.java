package com.centerflow.academic.courselevel.api;

import com.centerflow.academic.courselevel.application.CourseLevelPageResult;

import java.util.List;

public record CourseLevelPageResponse(
        List<CourseLevelResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static CourseLevelPageResponse from(
            CourseLevelPageResult result
    ) {
        List<CourseLevelResponse> content =
                result.content()
                        .stream()
                        .map(CourseLevelResponse::from)
                        .toList();

        return new CourseLevelPageResponse(
                content,
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}