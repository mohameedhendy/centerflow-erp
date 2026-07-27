package com.centerflow.academic.branch.application;

import org.springframework.data.domain.Page;

import java.util.List;

public record BranchPageResult(
        List<BranchResult> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static BranchPageResult from(
            Page<?> page,
            List<BranchResult> content
    ) {
        return new BranchPageResult(
                List.copyOf(content),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}