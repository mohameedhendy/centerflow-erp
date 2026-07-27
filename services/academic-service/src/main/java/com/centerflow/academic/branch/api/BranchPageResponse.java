package com.centerflow.academic.branch.api;

import com.centerflow.academic.branch.application.BranchPageResult;

import java.util.List;

public record BranchPageResponse(
        List<BranchResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static BranchPageResponse from(
            BranchPageResult result
    ) {
        List<BranchResponse> content =
                result.content()
                        .stream()
                        .map(BranchResponse::from)
                        .toList();

        return new BranchPageResponse(
                content,
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}