package com.centerflow.academic.courselevel.application;

import com.centerflow.academic.common.exception.CourseLevelNotFoundException;
import com.centerflow.academic.common.exception.CourseNotFoundException;
import com.centerflow.academic.common.exception.DuplicateCourseLevelException;
import com.centerflow.academic.common.exception.InactiveCourseException;
import com.centerflow.academic.common.exception.InvalidPaginationException;
import com.centerflow.academic.course.domain.Course;
import com.centerflow.academic.course.repository.CourseRepository;
import com.centerflow.academic.courselevel.domain.CourseLevel;
import com.centerflow.academic.courselevel.repository.CourseLevelRepository;
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
public class CourseLevelService {

    private static final int MAXIMUM_PAGE_SIZE = 100;

    private final CourseLevelRepository levelRepository;
    private final CourseRepository courseRepository;
    private final Clock clock;

    public CourseLevelService(
            CourseLevelRepository levelRepository,
            CourseRepository courseRepository,
            Clock clock
    ) {
        this.levelRepository = levelRepository;
        this.courseRepository = courseRepository;
        this.clock = clock;
    }

    @Transactional
    public CourseLevelResult create(
            UUID courseId,
            String code,
            String name,
            int sequenceNumber,
            int durationHours,
            String description
    ) {
        requireActiveCourse(courseId);

        String normalizedCode =
                CourseLevel.normalizedCode(code);

        boolean duplicateCode =
                levelRepository
                        .existsByCourseIdAndCode(
                                courseId,
                                normalizedCode
                        );

        boolean duplicateSequence =
                levelRepository
                        .existsByCourseIdAndSequenceNumber(
                                courseId,
                                sequenceNumber
                        );

        if (duplicateCode || duplicateSequence) {
            throw new DuplicateCourseLevelException(
                    courseId,
                    normalizedCode,
                    sequenceNumber
            );
        }

        CourseLevel level = CourseLevel.create(
                courseId,
                normalizedCode,
                name,
                sequenceNumber,
                durationHours,
                description,
                Instant.now(clock)
        );

        try {
            CourseLevel savedLevel =
                    levelRepository.saveAndFlush(
                            level
                    );

            return CourseLevelResult.from(savedLevel);

        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateCourseLevelException(
                    courseId,
                    normalizedCode,
                    sequenceNumber,
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public CourseLevelResult getById(UUID levelId) {
        return CourseLevelResult.from(
                findLevel(levelId)
        );
    }

    @Transactional(readOnly = true)
    public CourseLevelPageResult search(
            UUID courseId,
            String search,
            Boolean active,
            int page,
            int size
    ) {
        validatePagination(page, size);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc(
                                "sequenceNumber"
                        ),
                        Sort.Order.asc("name")
                )
        );

        Page<CourseLevel> levelPage =
                levelRepository.search(
                        courseId,
                        normalizeFilter(search),
                        active,
                        pageRequest
                );

        List<CourseLevelResult> content =
                levelPage.getContent()
                        .stream()
                        .map(CourseLevelResult::from)
                        .toList();

        return CourseLevelPageResult.from(
                levelPage,
                content
        );
    }

    @Transactional
    public CourseLevelResult update(
            UUID levelId,
            String name,
            int sequenceNumber,
            int durationHours,
            String description
    ) {
        CourseLevel level = findLevel(levelId);

        boolean duplicateSequence =
                levelRepository
                        .existsByCourseIdAndSequenceNumberAndIdNot(
                                level.getCourseId(),
                                sequenceNumber,
                                levelId
                        );

        if (duplicateSequence) {
            throw new DuplicateCourseLevelException(
                    level.getCourseId(),
                    level.getCode(),
                    sequenceNumber
            );
        }

        level.updateDetails(
                name,
                sequenceNumber,
                durationHours,
                description,
                Instant.now(clock)
        );

        try {
            levelRepository.flush();
            return CourseLevelResult.from(level);

        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateCourseLevelException(
                    level.getCourseId(),
                    level.getCode(),
                    sequenceNumber,
                    exception
            );
        }
    }

    @Transactional
    public CourseLevelResult changeStatus(
            UUID levelId,
            boolean active
    ) {
        CourseLevel level = findLevel(levelId);

        if (active) {
            requireActiveCourse(
                    level.getCourseId()
            );
        }

        level.changeStatus(
                active,
                Instant.now(clock)
        );

        return CourseLevelResult.from(level);
    }

    private CourseLevel findLevel(UUID levelId) {
        return levelRepository
                .findById(levelId)
                .orElseThrow(
                        () -> new CourseLevelNotFoundException(
                                levelId
                        )
                );
    }

    private Course requireActiveCourse(
            UUID courseId
    ) {
        Course course = courseRepository
                .findById(courseId)
                .orElseThrow(
                        () -> new CourseNotFoundException(
                                courseId
                        )
                );

        if (!course.isActive()) {
            throw new InactiveCourseException(
                    courseId
            );
        }

        return course;
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

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.strip();
    }
}