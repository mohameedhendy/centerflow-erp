package com.centerflow.notification.common.api;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(

        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last

) {

    public static <S, T> PageResponse<T> from(
            Page<S> sourcePage,
            Function<S, T> mapper
    ) {
        List<T> mappedContent = sourcePage
                .getContent()
                .stream()
                .map(mapper)
                .toList();

        return new PageResponse<>(
                mappedContent,
                sourcePage.getNumber(),
                sourcePage.getSize(),
                sourcePage.getTotalElements(),
                sourcePage.getTotalPages(),
                sourcePage.isFirst(),
                sourcePage.isLast()
        );
    }
}