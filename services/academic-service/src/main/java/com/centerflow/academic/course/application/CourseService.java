package com.centerflow.academic.course.application;

import com.centerflow.academic.common.exception.CourseNotFoundException;
import com.centerflow.academic.common.exception.DuplicateCourseCodeException;
import com.centerflow.academic.common.exception.InvalidPaginationException;
import com.centerflow.academic.course.domain.Course;
import com.centerflow.academic.course.repository.CourseRepository;
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
public class CourseService {

    private static final int MAXIMUM_PAGE_SIZE = 100;

    private final CourseRepository courseRepository;
    private final Clock clock;

    public CourseService(
            CourseRepository courseRepository,
            Clock clock
    ) {
        this.courseRepository = courseRepository;
        this.clock = clock;
    }

    @Transactional
    public CourseResult create(
            String code,
            String name,
            String description
    ) {
        String normalizedCode =
                Course.normalizedCode(code);

        if (courseRepository.existsByCode(
                normalizedCode
        )) {
            throw new DuplicateCourseCodeException(
                    normalizedCode
            );
        }

        Course course = Course.create(
                normalizedCode,
                name,
                description,
                Instant.now(clock)
        );

        try {
            Course savedCourse =
                    courseRepository.saveAndFlush(
                            course
                    );

            return CourseResult.from(savedCourse);

        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateCourseCodeException(
                    normalizedCode,
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public CourseResult getById(UUID courseId) {
        return CourseResult.from(
                findCourse(courseId)
        );
    }

    @Transactional(readOnly = true)
    public CoursePageResult search(
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
                        Sort.Order.asc("name"),
                        Sort.Order.asc("code")
                )
        );

        Page<Course> coursePage =
                courseRepository.search(
                        normalizeFilter(search),
                        active,
                        pageRequest
                );

        List<CourseResult> content =
                coursePage.getContent()
                        .stream()
                        .map(CourseResult::from)
                        .toList();

        return CoursePageResult.from(
                coursePage,
                content
        );
    }

    @Transactional
    public CourseResult update(
            UUID courseId,
            String name,
            String description
    ) {
        Course course = findCourse(courseId);

        course.updateDetails(
                name,
                description,
                Instant.now(clock)
        );

        return CourseResult.from(course);
    }

    @Transactional
    public CourseResult changeStatus(
            UUID courseId,
            boolean active
    ) {
        Course course = findCourse(courseId);

        course.changeStatus(
                active,
                Instant.now(clock)
        );

        return CourseResult.from(course);
    }

    private Course findCourse(UUID courseId) {
        return courseRepository
                .findById(courseId)
                .orElseThrow(
                        () -> new CourseNotFoundException(
                                courseId
                        )
                );
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