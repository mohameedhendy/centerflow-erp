package com.centerflow.academic.attendance.api;

import com.centerflow.academic.attendance.application.AttendanceEntryCommand;
import com.centerflow.academic.attendance.application.AttendanceMarkingResult;
import com.centerflow.academic.attendance.application.AttendancePageResult;
import com.centerflow.academic.attendance.application.AttendanceService;
import com.centerflow.academic.attendance.application.AttendanceSummaryResult;
import com.centerflow.academic.attendance.domain.AttendanceStatus;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/academic")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(
            AttendanceService attendanceService
    ) {
        this.attendanceService = attendanceService;
    }

    @PutMapping(
            "/batch-sessions/{sessionId}/attendance"
    )
    public AttendanceMarkingResponse markAttendance(
            @PathVariable UUID sessionId,

            @Valid @RequestBody
            MarkAttendanceRequest request
    ) {
        List<AttendanceEntryCommand> commands =
                request.records()
                        .stream()
                        .map(
                                record ->
                                        new AttendanceEntryCommand(
                                                record.enrollmentId(),
                                                record.studentId(),
                                                record.status(),
                                                record.notes()
                                        )
                        )
                        .toList();

        AttendanceMarkingResult result =
                attendanceService.markAttendance(
                        sessionId,
                        commands
                );

        return AttendanceMarkingResponse.from(
                result
        );
    }

    @GetMapping(
            "/batch-sessions/{sessionId}/attendance"
    )
    public AttendancePageResponse searchBySession(
            @PathVariable UUID sessionId,

            @RequestParam(required = false)
            AttendanceStatus status,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        AttendancePageResult result =
                attendanceService.searchBySession(
                        sessionId,
                        status,
                        page,
                        size
                );

        return AttendancePageResponse.from(result);
    }

    @GetMapping(
            "/batch-sessions/{sessionId}/attendance/summary"
    )
    public AttendanceSummaryResponse getSummary(
            @PathVariable UUID sessionId
    ) {
        AttendanceSummaryResult result =
                attendanceService.getSummary(
                        sessionId
                );

        return AttendanceSummaryResponse.from(
                result
        );
    }

    @GetMapping(
            "/batches/{batchId}/attendance"
    )
    public AttendancePageResponse searchByBatch(
            @PathVariable UUID batchId,

            @RequestParam(required = false)
            UUID studentId,

            @RequestParam(required = false)
            UUID enrollmentId,

            @RequestParam(required = false)
            AttendanceStatus status,

            @RequestParam(required = false)
            LocalDate dateFrom,

            @RequestParam(required = false)
            LocalDate dateTo,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        AttendancePageResult result =
                attendanceService.searchByBatch(
                        batchId,
                        studentId,
                        enrollmentId,
                        status,
                        dateFrom,
                        dateTo,
                        page,
                        size
                );

        return AttendancePageResponse.from(result);
    }
}