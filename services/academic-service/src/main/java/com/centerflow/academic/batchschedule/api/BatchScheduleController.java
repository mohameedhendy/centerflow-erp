package com.centerflow.academic.batchschedule.api;

import com.centerflow.academic.batchschedule.application.BatchSchedulePageResult;
import com.centerflow.academic.batchschedule.application.BatchScheduleResult;
import com.centerflow.academic.batchschedule.application.BatchScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/academic")
public class BatchScheduleController {

    private final BatchScheduleService scheduleService;

    public BatchScheduleController(
            BatchScheduleService scheduleService
    ) {
        this.scheduleService = scheduleService;
    }

    @PostMapping("/batches/{batchId}/schedules")
    @ResponseStatus(HttpStatus.CREATED)
    public BatchScheduleResponse create(
            @PathVariable UUID batchId,

            @Valid @RequestBody
            CreateBatchScheduleRequest request
    ) {
        BatchScheduleResult result =
                scheduleService.create(
                        batchId,
                        request.dayOfWeek(),
                        request.startTime(),
                        request.endTime()
                );

        return BatchScheduleResponse.from(result);
    }

    @GetMapping("/batches/{batchId}/schedules")
    public BatchSchedulePageResponse search(
            @PathVariable UUID batchId,

            @RequestParam(required = false)
            DayOfWeek dayOfWeek,

            @RequestParam(required = false)
            Boolean active,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        BatchSchedulePageResult result =
                scheduleService.search(
                        batchId,
                        dayOfWeek,
                        active,
                        page,
                        size
                );

        return BatchSchedulePageResponse.from(result);
    }

    @GetMapping("/batch-schedules/{scheduleId}")
    public BatchScheduleResponse getById(
            @PathVariable UUID scheduleId
    ) {
        return BatchScheduleResponse.from(
                scheduleService.getById(scheduleId)
        );
    }

    @PutMapping("/batch-schedules/{scheduleId}")
    public BatchScheduleResponse update(
            @PathVariable UUID scheduleId,

            @Valid @RequestBody
            UpdateBatchScheduleRequest request
    ) {
        BatchScheduleResult result =
                scheduleService.update(
                        scheduleId,
                        request.dayOfWeek(),
                        request.startTime(),
                        request.endTime()
                );

        return BatchScheduleResponse.from(result);
    }

    @PatchMapping(
            "/batch-schedules/{scheduleId}/status"
    )
    public BatchScheduleResponse changeStatus(
            @PathVariable UUID scheduleId,

            @Valid @RequestBody
            ChangeBatchScheduleStatusRequest request
    ) {
        BatchScheduleResult result =
                scheduleService.changeStatus(
                        scheduleId,
                        request.active()
                );

        return BatchScheduleResponse.from(result);
    }
}