package com.centerflow.academic.classroom.application;

import com.centerflow.academic.branch.domain.Branch;
import com.centerflow.academic.branch.repository.BranchRepository;
import com.centerflow.academic.classroom.domain.Classroom;
import com.centerflow.academic.classroom.repository.ClassroomRepository;
import com.centerflow.academic.common.exception.BranchNotFoundException;
import com.centerflow.academic.common.exception.ClassroomNotFoundException;
import com.centerflow.academic.common.exception.DuplicateClassroomCodeException;
import com.centerflow.academic.common.exception.InactiveBranchException;
import com.centerflow.academic.common.exception.InvalidCapacityRangeException;
import com.centerflow.academic.common.exception.InvalidPaginationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ClassroomService {

    private static final int MAXIMUM_PAGE_SIZE = 100;
    private static final int MAXIMUM_CAPACITY = 1000;

    private final ClassroomRepository classroomRepository;
    private final BranchRepository branchRepository;
    private final Clock clock;

    public ClassroomService(
            ClassroomRepository classroomRepository,
            BranchRepository branchRepository,
            Clock clock
    ) {
        this.classroomRepository = classroomRepository;
        this.branchRepository = branchRepository;
        this.clock = clock;
    }

    @Transactional
    public ClassroomResult create(
            UUID branchId,
            String code,
            String name,
            int capacity,
            String floor
    ) {
        requireActiveBranch(branchId);

        String normalizedCode =
                Classroom.normalizedCode(code);

        if (classroomRepository
                .existsByBranchIdAndCode(
                        branchId,
                        normalizedCode
                )) {
            throw new DuplicateClassroomCodeException(
                    branchId,
                    normalizedCode
            );
        }

        Classroom classroom = Classroom.create(
                branchId,
                normalizedCode,
                name,
                capacity,
                floor,
                Instant.now(clock)
        );

        try {
            Classroom savedClassroom =
                    classroomRepository.saveAndFlush(
                            classroom
                    );

            return ClassroomResult.from(
                    savedClassroom
            );

        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateClassroomCodeException(
                    branchId,
                    normalizedCode,
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public ClassroomResult getById(
            UUID classroomId
    ) {
        return ClassroomResult.from(
                findClassroom(classroomId)
        );
    }

    @Transactional(readOnly = true)
    public ClassroomPageResult search(
            UUID branchId,
            String search,
            Integer minimumCapacity,
            Integer maximumCapacity,
            Boolean active,
            int page,
            int size
    ) {
        validatePagination(page, size);

        validateCapacityRange(
                minimumCapacity,
                maximumCapacity
        );

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("name"),
                        Sort.Order.asc("code")
                )
        );

        Page<Classroom> classroomPage =
                classroomRepository.search(
                        branchId,
                        normalizeFilter(search),
                        minimumCapacity,
                        maximumCapacity,
                        active,
                        pageRequest
                );

        List<ClassroomResult> content =
                classroomPage.getContent()
                        .stream()
                        .map(ClassroomResult::from)
                        .toList();

        return ClassroomPageResult.from(
                classroomPage,
                content
        );
    }

    @Transactional
    public ClassroomResult update(
            UUID classroomId,
            String name,
            int capacity,
            String floor
    ) {
        Classroom classroom =
                findClassroom(classroomId);

        classroom.updateDetails(
                name,
                capacity,
                floor,
                Instant.now(clock)
        );

        return ClassroomResult.from(classroom);
    }

    @Transactional
    public ClassroomResult changeStatus(
            UUID classroomId,
            boolean active
    ) {
        Classroom classroom =
                findClassroom(classroomId);

        if (active) {
            requireActiveBranch(
                    classroom.getBranchId()
            );
        }

        classroom.changeStatus(
                active,
                Instant.now(clock)
        );

        return ClassroomResult.from(classroom);
    }

    private Classroom findClassroom(
            UUID classroomId
    ) {
        return classroomRepository
                .findById(classroomId)
                .orElseThrow(
                        () -> new ClassroomNotFoundException(
                                classroomId
                        )
                );
    }

    private Branch requireActiveBranch(
            UUID branchId
    ) {
        Branch branch = branchRepository
                .findById(branchId)
                .orElseThrow(
                        () -> new BranchNotFoundException(
                                branchId
                        )
                );

        if (!branch.isActive()) {
            throw new InactiveBranchException(
                    branchId
            );
        }

        return branch;
    }

    private void validatePagination(
            int page,
            int size
    ) {
        if (page < 0) {
            throw new InvalidPaginationException(
                    "Page number must not be negative"
            );
        }

        if (size < 1 || size > MAXIMUM_PAGE_SIZE) {
            throw new InvalidPaginationException(
                    "Page size must be between 1 and "
                            + MAXIMUM_PAGE_SIZE
            );
        }
    }

    private void validateCapacityRange(
            Integer minimumCapacity,
            Integer maximumCapacity
    ) {
        if (minimumCapacity != null
                && (
                minimumCapacity < 1
                        || minimumCapacity
                        > MAXIMUM_CAPACITY
        )) {
            throw new InvalidCapacityRangeException(
                    "Minimum capacity must be between 1 and "
                            + MAXIMUM_CAPACITY
            );
        }

        if (maximumCapacity != null
                && (
                maximumCapacity < 1
                        || maximumCapacity
                        > MAXIMUM_CAPACITY
        )) {
            throw new InvalidCapacityRangeException(
                    "Maximum capacity must be between 1 and "
                            + MAXIMUM_CAPACITY
            );
        }

        if (minimumCapacity != null
                && maximumCapacity != null
                && minimumCapacity > maximumCapacity) {
            throw new InvalidCapacityRangeException(
                    "Minimum capacity must not exceed maximum capacity"
            );
        }
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.strip();
    }
}