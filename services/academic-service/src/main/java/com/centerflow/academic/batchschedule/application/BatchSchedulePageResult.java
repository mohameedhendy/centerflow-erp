package com.centerflow.academic.batchschedule.application;

import org.springframework.data.domain.Page;

import java.util.List;

public record BatchSchedulePageResult(
        List<BatchScheduleResult> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static BatchSchedulePageResult from(
            Page<?> page,
            List<BatchScheduleResult> content
    ) {
        return new BatchSchedulePageResult(
                List.copyOf(content),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}