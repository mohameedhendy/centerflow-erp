package com.centerflow.academic.classroom.api;

import com.centerflow.academic.classroom.application.ClassroomPageResult;
import com.centerflow.academic.classroom.application.ClassroomResult;
import com.centerflow.academic.classroom.application.ClassroomService;
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
@RequestMapping("/api/v1/academic/classrooms")
public class ClassroomController {

    private final ClassroomService classroomService;

    public ClassroomController(
            ClassroomService classroomService
    ) {
        this.classroomService = classroomService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClassroomResponse create(
            @Valid @RequestBody
            CreateClassroomRequest request
    ) {
        ClassroomResult result =
                classroomService.create(
                        request.branchId(),
                        request.code(),
                        request.name(),
                        request.capacity(),
                        request.floor()
                );

        return ClassroomResponse.from(result);
    }

    @GetMapping("/{classroomId}")
    public ClassroomResponse getById(
            @PathVariable UUID classroomId
    ) {
        return ClassroomResponse.from(
                classroomService.getById(
                        classroomId
                )
        );
    }

    @GetMapping
    public ClassroomPageResponse search(
            @RequestParam(required = false)
            UUID branchId,

            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            Integer minimumCapacity,

            @RequestParam(required = false)
            Integer maximumCapacity,

            @RequestParam(required = false)
            Boolean active,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        ClassroomPageResult result =
                classroomService.search(
                        branchId,
                        search,
                        minimumCapacity,
                        maximumCapacity,
                        active,
                        page,
                        size
                );

        return ClassroomPageResponse.from(result);
    }

    @PutMapping("/{classroomId}")
    public ClassroomResponse update(
            @PathVariable UUID classroomId,

            @Valid @RequestBody
            UpdateClassroomRequest request
    ) {
        ClassroomResult result =
                classroomService.update(
                        classroomId,
                        request.name(),
                        request.capacity(),
                        request.floor()
                );

        return ClassroomResponse.from(result);
    }

    @PatchMapping("/{classroomId}/status")
    public ClassroomResponse changeStatus(
            @PathVariable UUID classroomId,

            @Valid @RequestBody
            ChangeClassroomStatusRequest request
    ) {
        ClassroomResult result =
                classroomService.changeStatus(
                        classroomId,
                        request.active()
                );

        return ClassroomResponse.from(result);
    }
}