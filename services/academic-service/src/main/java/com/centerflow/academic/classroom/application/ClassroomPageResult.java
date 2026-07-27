package com.centerflow.academic.classroom.application;

import org.springframework.data.domain.Page;

import java.util.List;

public record ClassroomPageResult(
        List<ClassroomResult> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static ClassroomPageResult from(
            Page<?> page,
            List<ClassroomResult> content
    ) {
        return new ClassroomPageResult(
                List.copyOf(content),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}