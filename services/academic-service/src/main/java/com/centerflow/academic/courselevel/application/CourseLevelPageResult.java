package com.centerflow.academic.courselevel.application;

import org.springframework.data.domain.Page;

import java.util.List;

public record CourseLevelPageResult(
        List<CourseLevelResult> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static CourseLevelPageResult from(
            Page<?> page,
            List<CourseLevelResult> content
    ) {
        return new CourseLevelPageResult(
                List.copyOf(content),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}