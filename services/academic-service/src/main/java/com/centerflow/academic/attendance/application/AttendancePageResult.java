package com.centerflow.academic.attendance.application;

import org.springframework.data.domain.Page;

import java.util.List;

public record AttendancePageResult(
        List<AttendanceRecordResult> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static AttendancePageResult from(
            Page<?> page,
            List<AttendanceRecordResult> content
    ) {
        return new AttendancePageResult(
                List.copyOf(content),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}