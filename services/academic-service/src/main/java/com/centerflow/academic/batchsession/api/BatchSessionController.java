package com.centerflow.academic.batchsession.api;

import com.centerflow.academic.batchsession.application.BatchSessionPageResult;
import com.centerflow.academic.batchsession.application.BatchSessionResult;
import com.centerflow.academic.batchsession.application.BatchSessionService;
import com.centerflow.academic.batchsession.application.SessionGenerationResult;
import com.centerflow.academic.batchsession.domain.BatchSessionStatus;
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
@RequestMapping("/api/v1/academic")
public class BatchSessionController {

    private final BatchSessionService sessionService;

    public BatchSessionController(
            BatchSessionService sessionService
    ) {
        this.sessionService = sessionService;
    }

    @PostMapping("/batches/{batchId}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public BatchSessionResponse create(
            @PathVariable UUID batchId,
            @Valid @RequestBody
            CreateBatchSessionRequest request
    ) {
        BatchSessionResult result =
                sessionService.create(
                        batchId,
                        request.sessionDate(),
                        request.startTime(),
                        request.endTime(),
                        request.topic()
                );

        return BatchSessionResponse.from(result);
    }

    @PostMapping("/batches/{batchId}/sessions/generate")
    public SessionGenerationResponse generate(
            @PathVariable UUID batchId,
            @RequestBody(required = false)
            GenerateBatchSessionsRequest request
    ) {
        LocalDate dateFrom =
                request == null ? null : request.dateFrom();

        LocalDate dateTo =
                request == null ? null : request.dateTo();

        SessionGenerationResult result =
                sessionService.generate(
                        batchId,
                        dateFrom,
                        dateTo
                );

        return SessionGenerationResponse.from(result);
    }

    @GetMapping("/batches/{batchId}/sessions")
    public BatchSessionPageResponse search(
            @PathVariable UUID batchId,

            @RequestParam(required = false)
            LocalDate dateFrom,

            @RequestParam(required = false)
            LocalDate dateTo,

            @RequestParam(required = false)
            BatchSessionStatus status,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        BatchSessionPageResult result =
                sessionService.search(
                        batchId,
                        dateFrom,
                        dateTo,
                        status,
                        page,
                        size
                );

        return BatchSessionPageResponse.from(result);
    }

    @GetMapping("/batch-sessions/{sessionId}")
    public BatchSessionResponse getById(
            @PathVariable UUID sessionId
    ) {
        return BatchSessionResponse.from(
                sessionService.getById(sessionId)
        );
    }

    @PutMapping("/batch-sessions/{sessionId}")
    public BatchSessionResponse update(
            @PathVariable UUID sessionId,
            @Valid @RequestBody
            UpdateBatchSessionRequest request
    ) {
        BatchSessionResult result =
                sessionService.update(
                        sessionId,
                        request.sessionDate(),
                        request.startTime(),
                        request.endTime(),
                        request.topic()
                );

        return BatchSessionResponse.from(result);
    }

    @PatchMapping("/batch-sessions/{sessionId}/status")
    public BatchSessionResponse changeStatus(
            @PathVariable UUID sessionId,
            @Valid @RequestBody
            ChangeBatchSessionStatusRequest request
    ) {
        BatchSessionResult result =
                sessionService.changeStatus(
                        sessionId,
                        request.status()
                );

        return BatchSessionResponse.from(result);
    }
}