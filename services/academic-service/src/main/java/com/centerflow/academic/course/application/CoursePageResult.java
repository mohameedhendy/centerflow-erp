package com.centerflow.academic.course.application;

import org.springframework.data.domain.Page;

import java.util.List;

public record CoursePageResult(
        List<CourseResult> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static CoursePageResult from(
            Page<?> page,
            List<CourseResult> content
    ) {
        return new CoursePageResult(
                List.copyOf(content),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}