package com.centerflow.academic.instructor.api;

import com.centerflow.academic.instructor.application.InstructorPageResult;
import com.centerflow.academic.instructor.application.InstructorResult;
import com.centerflow.academic.instructor.application.InstructorService;
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
@RequestMapping("/api/v1/academic/instructors")
public class InstructorController {

    private final InstructorService instructorService;

    public InstructorController(
            InstructorService instructorService
    ) {
        this.instructorService = instructorService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstructorResponse create(
            @Valid @RequestBody
            CreateInstructorRequest request
    ) {
        InstructorResult result =
                instructorService.create(
                        request.code(),
                        request.firstName(),
                        request.lastName(),
                        request.email(),
                        request.phone(),
                        request.specialization(),
                        request.bio()
                );

        return InstructorResponse.from(result);
    }

    @GetMapping("/{instructorId}")
    public InstructorResponse getById(
            @PathVariable UUID instructorId
    ) {
        return InstructorResponse.from(
                instructorService.getById(
                        instructorId
                )
        );
    }

    @GetMapping
    public InstructorPageResponse search(
            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            String specialization,

            @RequestParam(required = false)
            Boolean active,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        InstructorPageResult result =
                instructorService.search(
                        search,
                        specialization,
                        active,
                        page,
                        size
                );

        return InstructorPageResponse.from(result);
    }

    @PutMapping("/{instructorId}")
    public InstructorResponse update(
            @PathVariable UUID instructorId,

            @Valid @RequestBody
            UpdateInstructorRequest request
    ) {
        InstructorResult result =
                instructorService.update(
                        instructorId,
                        request.firstName(),
                        request.lastName(),
                        request.email(),
                        request.phone(),
                        request.specialization(),
                        request.bio()
                );

        return InstructorResponse.from(result);
    }

    @PatchMapping("/{instructorId}/status")
    public InstructorResponse changeStatus(
            @PathVariable UUID instructorId,

            @Valid @RequestBody
            ChangeInstructorStatusRequest request
    ) {
        InstructorResult result =
                instructorService.changeStatus(
                        instructorId,
                        request.active()
                );

        return InstructorResponse.from(result);
    }
}