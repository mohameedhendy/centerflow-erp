package com.centerflow.academic.batchschedule.api;

import com.centerflow.academic.batchschedule.application.BatchSchedulePageResult;

import java.util.List;

public record BatchSchedulePageResponse(
        List<BatchScheduleResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static BatchSchedulePageResponse from(
            BatchSchedulePageResult result
    ) {
        List<BatchScheduleResponse> content =
                result.content()
                        .stream()
                        .map(BatchScheduleResponse::from)
                        .toList();

        return new BatchSchedulePageResponse(
                content,
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}