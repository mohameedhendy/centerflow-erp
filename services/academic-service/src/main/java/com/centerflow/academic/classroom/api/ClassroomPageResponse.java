package com.centerflow.academic.classroom.api;

import com.centerflow.academic.classroom.application.ClassroomPageResult;

import java.util.List;

public record ClassroomPageResponse(
        List<ClassroomResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static ClassroomPageResponse from(
            ClassroomPageResult result
    ) {
        List<ClassroomResponse> content =
                result.content()
                        .stream()
                        .map(ClassroomResponse::from)
                        .toList();

        return new ClassroomPageResponse(
                content,
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}