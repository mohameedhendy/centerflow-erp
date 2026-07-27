package com.centerflow.academic.branch.api;

import com.centerflow.academic.branch.application.BranchPageResult;
import com.centerflow.academic.branch.application.BranchResult;
import com.centerflow.academic.branch.application.BranchService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/academic/branches")
public class BranchController {

    private final BranchService branchService;

    public BranchController(
            BranchService branchService
    ) {
        this.branchService = branchService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BranchResponse create(
            @Valid @RequestBody
            CreateBranchRequest request
    ) {
        BranchResult result = branchService.create(
                request.code(),
                request.name(),
                request.phone(),
                request.email(),
                request.address(),
                request.city()
        );

        return BranchResponse.from(result);
    }

    @GetMapping("/{branchId}")
    public BranchResponse getById(
            @PathVariable UUID branchId
    ) {
        return BranchResponse.from(
                branchService.getById(branchId)
        );
    }

    @GetMapping
    public BranchPageResponse search(
            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            String city,

            @RequestParam(required = false)
            Boolean active,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        BranchPageResult result =
                branchService.search(
                        search,
                        city,
                        active,
                        page,
                        size
                );

        return BranchPageResponse.from(result);
    }

    @PutMapping("/{branchId}")
    public BranchResponse update(
            @PathVariable UUID branchId,

            @Valid @RequestBody
            UpdateBranchRequest request
    ) {
        BranchResult result = branchService.update(
                branchId,
                request.name(),
                request.phone(),
                request.email(),
                request.address(),
                request.city()
        );

        return BranchResponse.from(result);
    }

    @PatchMapping("/{branchId}/status")
    public BranchResponse changeStatus(
            @PathVariable UUID branchId,

            @Valid @RequestBody
            ChangeBranchStatusRequest request
    ) {
        BranchResult result =
                branchService.changeStatus(
                        branchId,
                        request.active()
                );

        return BranchResponse.from(result);
    }
}