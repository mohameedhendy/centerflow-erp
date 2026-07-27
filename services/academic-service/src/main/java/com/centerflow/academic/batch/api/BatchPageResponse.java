package com.centerflow.academic.batch.api;

import com.centerflow.academic.batch.application.BatchPageResult;

import java.util.List;

public record BatchPageResponse(
        List<BatchResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static BatchPageResponse from(
            BatchPageResult result
    ) {
        List<BatchResponse> content =
                result.content()
                        .stream()
                        .map(BatchResponse::from)
                        .toList();

        return new BatchPageResponse(
                content,
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}