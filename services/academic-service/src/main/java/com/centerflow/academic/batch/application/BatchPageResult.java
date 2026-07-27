package com.centerflow.academic.batch.application;

import org.springframework.data.domain.Page;

import java.util.List;

public record BatchPageResult(
        List<BatchResult> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static BatchPageResult from(
            Page<?> page,
            List<BatchResult> content
    ) {
        return new BatchPageResult(
                List.copyOf(content),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}