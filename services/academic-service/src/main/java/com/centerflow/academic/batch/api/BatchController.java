package com.centerflow.academic.batch.api;

import com.centerflow.academic.batch.application.BatchPageResult;
import com.centerflow.academic.batch.application.BatchResult;
import com.centerflow.academic.batch.application.BatchService;
import com.centerflow.academic.batch.domain.BatchStatus;
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

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/academic/batches")
public class BatchController {

    private final BatchService batchService;

    public BatchController(
            BatchService batchService
    ) {
        this.batchService = batchService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BatchResponse create(
            @Valid @RequestBody
            CreateBatchRequest request
    ) {
        BatchResult result = batchService.create(
                request.code(),
                request.name(),
                request.branchId(),
                request.classroomId(),
                request.courseLevelId(),
                request.instructorId(),
                request.capacity(),
                request.startDate(),
                request.endDate()
        );

        return BatchResponse.from(result);
    }

    @GetMapping("/{batchId}")
    public BatchResponse getById(
            @PathVariable UUID batchId
    ) {
        return BatchResponse.from(
                batchService.getById(batchId)
        );
    }

    @GetMapping
    public BatchPageResponse search(
            @RequestParam(required = false)
            UUID branchId,

            @RequestParam(required = false)
            UUID classroomId,

            @RequestParam(required = false)
            UUID courseLevelId,

            @RequestParam(required = false)
            UUID instructorId,

            @RequestParam(required = false)
            BatchStatus status,

            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            LocalDate startDateFrom,

            @RequestParam(required = false)
            LocalDate startDateTo,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        BatchPageResult result =
                batchService.search(
                        branchId,
                        classroomId,
                        courseLevelId,
                        instructorId,
                        status,
                        search,
                        startDateFrom,
                        startDateTo,
                        page,
                        size
                );

        return BatchPageResponse.from(result);
    }

    @PutMapping("/{batchId}")
    public BatchResponse update(
            @PathVariable UUID batchId,

            @Valid @RequestBody
            UpdateBatchRequest request
    ) {
        BatchResult result = batchService.update(
                batchId,
                request.name(),
                request.branchId(),
                request.classroomId(),
                request.courseLevelId(),
                request.instructorId(),
                request.capacity(),
                request.startDate(),
                request.endDate()
        );

        return BatchResponse.from(result);
    }

    @PatchMapping("/{batchId}/status")
    public BatchResponse changeStatus(
            @PathVariable UUID batchId,

            @Valid @RequestBody
            ChangeBatchStatusRequest request
    ) {
        BatchResult result =
                batchService.changeStatus(
                        batchId,
                        request.status()
                );

        return BatchResponse.from(result);
    }
}