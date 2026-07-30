package com.centerflow.finance.earning.api;

import com.centerflow.finance.common.api.PageResponse;
import com.centerflow.finance.earning.api.dto.CancelInstructorEarningRequest;
import com.centerflow.finance.earning.api.dto.CreateInstructorEarningRequest;
import com.centerflow.finance.earning.api.dto.InstructorEarningResponse;
import com.centerflow.finance.earning.api.dto.PayInstructorEarningRequest;
import com.centerflow.finance.earning.application.InstructorEarningService;
import com.centerflow.finance.earning.domain.InstructorEarningStatus;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping(
        "/api/v1/finance/instructor-earnings"
)
public class InstructorEarningController {

    private final InstructorEarningService
            earningService;

    public InstructorEarningController(
            InstructorEarningService earningService
    ) {
        this.earningService = earningService;
    }

    @PostMapping
    public ResponseEntity<InstructorEarningResponse>
    recordEarning(
            @Valid
            @RequestBody
            CreateInstructorEarningRequest request
    ) {
        InstructorEarningResponse response =
                earningService.recordEarning(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{earningId}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping("/{earningId}")
    public ResponseEntity<InstructorEarningResponse>
    getEarning(
            @PathVariable UUID earningId
    ) {
        return ResponseEntity.ok(
                earningService.getEarning(earningId)
        );
    }

    @PostMapping("/{earningId}/pay")
    public ResponseEntity<InstructorEarningResponse>
    payEarning(
            @PathVariable UUID earningId,

            @Valid
            @RequestBody
            PayInstructorEarningRequest request
    ) {
        return ResponseEntity.ok(
                earningService.payEarning(
                        earningId,
                        request
                )
        );
    }

    @PostMapping("/{earningId}/cancel")
    public ResponseEntity<InstructorEarningResponse>
    cancelEarning(
            @PathVariable UUID earningId,

            @Valid
            @RequestBody
            CancelInstructorEarningRequest request
    ) {
        return ResponseEntity.ok(
                earningService.cancelEarning(
                        earningId,
                        request
                )
        );
    }

    @GetMapping
    public ResponseEntity<
            PageResponse<InstructorEarningResponse>
            >
    searchEarnings(
            @RequestParam(required = false)
            UUID instructorId,

            @RequestParam(required = false)
            UUID batchId,

            @RequestParam(required = false)
            InstructorEarningStatus status,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate toDate,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        return ResponseEntity.ok(
                earningService.searchEarnings(
                        instructorId,
                        batchId,
                        status,
                        fromDate,
                        toDate,
                        page,
                        size
                )
        );
    }
}