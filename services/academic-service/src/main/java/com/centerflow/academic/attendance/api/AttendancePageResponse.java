package com.centerflow.academic.attendance.api;

import com.centerflow.academic.attendance.application.AttendancePageResult;

import java.util.List;

public record AttendancePageResponse(
        List<AttendanceRecordResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static AttendancePageResponse from(
            AttendancePageResult result
    ) {
        List<AttendanceRecordResponse> content =
                result.content()
                        .stream()
                        .map(
                                AttendanceRecordResponse
                                        ::from
                        )
                        .toList();

        return new AttendancePageResponse(
                content,
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}