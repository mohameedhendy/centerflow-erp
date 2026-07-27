package com.centerflow.academic.batchsession.application;

import org.springframework.data.domain.Page;

import java.util.List;

public record BatchSessionPageResult(
        List<BatchSessionResult> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static BatchSessionPageResult from(
            Page<?> page,
            List<BatchSessionResult> content
    ) {
        return new BatchSessionPageResult(
                List.copyOf(content),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}