package com.centerflow.academic.batchsession.api;

import com.centerflow.academic.batchsession.application.BatchSessionPageResult;

import java.util.List;

public record BatchSessionPageResponse(
        List<BatchSessionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static BatchSessionPageResponse from(
            BatchSessionPageResult result
    ) {
        List<BatchSessionResponse> content =
                result.content()
                        .stream()
                        .map(BatchSessionResponse::from)
                        .toList();

        return new BatchSessionPageResponse(
                content,
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}