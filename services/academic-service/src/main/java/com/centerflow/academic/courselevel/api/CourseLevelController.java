package com.centerflow.academic.courselevel.api;

import com.centerflow.academic.courselevel.application.CourseLevelPageResult;
import com.centerflow.academic.courselevel.application.CourseLevelResult;
import com.centerflow.academic.courselevel.application.CourseLevelService;
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
@RequestMapping("/api/v1/academic/course-levels")
public class CourseLevelController {

    private final CourseLevelService levelService;

    public CourseLevelController(
            CourseLevelService levelService
    ) {
        this.levelService = levelService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseLevelResponse create(
            @Valid @RequestBody
            CreateCourseLevelRequest request
    ) {
        CourseLevelResult result =
                levelService.create(
                        request.courseId(),
                        request.code(),
                        request.name(),
                        request.sequenceNumber(),
                        request.durationHours(),
                        request.description()
                );

        return CourseLevelResponse.from(result);
    }

    @GetMapping("/{levelId}")
    public CourseLevelResponse getById(
            @PathVariable UUID levelId
    ) {
        return CourseLevelResponse.from(
                levelService.getById(levelId)
        );
    }

    @GetMapping
    public CourseLevelPageResponse search(
            @RequestParam(required = false)
            UUID courseId,

            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            Boolean active,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        CourseLevelPageResult result =
                levelService.search(
                        courseId,
                        search,
                        active,
                        page,
                        size
                );

        return CourseLevelPageResponse.from(result);
    }

    @PutMapping("/{levelId}")
    public CourseLevelResponse update(
            @PathVariable UUID levelId,

            @Valid @RequestBody
            UpdateCourseLevelRequest request
    ) {
        CourseLevelResult result =
                levelService.update(
                        levelId,
                        request.name(),
                        request.sequenceNumber(),
                        request.durationHours(),
                        request.description()
                );

        return CourseLevelResponse.from(result);
    }

    @PatchMapping("/{levelId}/status")
    public CourseLevelResponse changeStatus(
            @PathVariable UUID levelId,

            @Valid @RequestBody
            ChangeCourseLevelStatusRequest request
    ) {
        CourseLevelResult result =
                levelService.changeStatus(
                        levelId,
                        request.active()
                );

        return CourseLevelResponse.from(result);
    }
}