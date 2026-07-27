package com.centerflow.academic.instructor.application;

import org.springframework.data.domain.Page;

import java.util.List;

public record InstructorPageResult(
        List<InstructorResult> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static InstructorPageResult from(
            Page<?> page,
            List<InstructorResult> content
    ) {
        return new InstructorPageResult(
                List.copyOf(content),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}