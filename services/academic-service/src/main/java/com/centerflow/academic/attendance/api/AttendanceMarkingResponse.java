package com.centerflow.academic.attendance.api;

import com.centerflow.academic.attendance.application.AttendanceMarkingResult;

import java.util.List;

public record AttendanceMarkingResponse(
        List<AttendanceRecordResponse> records
) {

    public static AttendanceMarkingResponse from(
            AttendanceMarkingResult result
    ) {
        List<AttendanceRecordResponse> records =
                result.records()
                        .stream()
                        .map(
                                AttendanceRecordResponse
                                        ::from
                        )
                        .toList();

        return new AttendanceMarkingResponse(
                records
        );
    }
}