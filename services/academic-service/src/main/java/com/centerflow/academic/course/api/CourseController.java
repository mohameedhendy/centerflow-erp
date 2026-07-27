package com.centerflow.academic.course.api;

import com.centerflow.academic.course.application.CoursePageResult;
import com.centerflow.academic.course.application.CourseResult;
import com.centerflow.academic.course.application.CourseService;
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
@RequestMapping("/api/v1/academic/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(
            CourseService courseService
    ) {
        this.courseService = courseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(
            @Valid @RequestBody
            CreateCourseRequest request
    ) {
        CourseResult result =
                courseService.create(
                        request.code(),
                        request.name(),
                        request.description()
                );

        return CourseResponse.from(result);
    }

    @GetMapping("/{courseId}")
    public CourseResponse getById(
            @PathVariable UUID courseId
    ) {
        return CourseResponse.from(
                courseService.getById(courseId)
        );
    }

    @GetMapping
    public CoursePageResponse search(
            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            Boolean active,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        CoursePageResult result =
                courseService.search(
                        search,
                        active,
                        page,
                        size
                );

        return CoursePageResponse.from(result);
    }

    @PutMapping("/{courseId}")
    public CourseResponse update(
            @PathVariable UUID courseId,

            @Valid @RequestBody
            UpdateCourseRequest request
    ) {
        CourseResult result =
                courseService.update(
                        courseId,
                        request.name(),
                        request.description()
                );

        return CourseResponse.from(result);
    }

    @PatchMapping("/{courseId}/status")
    public CourseResponse changeStatus(
            @PathVariable UUID courseId,

            @Valid @RequestBody
            ChangeCourseStatusRequest request
    ) {
        CourseResult result =
                courseService.changeStatus(
                        courseId,
                        request.active()
                );

        return CourseResponse.from(result);
    }
}