package com.centerflow.academic.instructor.application;

import com.centerflow.academic.common.exception.DuplicateInstructorException;
import com.centerflow.academic.common.exception.InstructorNotFoundException;
import com.centerflow.academic.common.exception.InvalidPaginationException;
import com.centerflow.academic.instructor.domain.Instructor;
import com.centerflow.academic.instructor.repository.InstructorRepository;
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
public class InstructorService {

    private static final int MAXIMUM_PAGE_SIZE = 100;

    private final InstructorRepository instructorRepository;
    private final Clock clock;

    public InstructorService(
            InstructorRepository instructorRepository,
            Clock clock
    ) {
        this.instructorRepository =
                instructorRepository;

        this.clock = clock;
    }

    @Transactional
    public InstructorResult create(
            String code,
            String firstName,
            String lastName,
            String email,
            String phone,
            String specialization,
            String bio
    ) {
        String normalizedCode =
                Instructor.normalizedCode(code);

        String normalizedEmail =
                Instructor.normalizedEmail(email);

        if (instructorRepository.existsByCode(
                normalizedCode
        )) {
            throw DuplicateInstructorException
                    .forCode(normalizedCode);
        }

        if (normalizedEmail != null
                && instructorRepository.existsByEmail(
                normalizedEmail
        )) {
            throw DuplicateInstructorException
                    .forEmail(normalizedEmail);
        }

        Instructor instructor = Instructor.create(
                normalizedCode,
                firstName,
                lastName,
                normalizedEmail,
                phone,
                specialization,
                bio,
                Instant.now(clock)
        );

        try {
            Instructor savedInstructor =
                    instructorRepository
                            .saveAndFlush(instructor);

            return InstructorResult.from(
                    savedInstructor
            );

        } catch (DataIntegrityViolationException exception) {
            throw DuplicateInstructorException
                    .forConflictingData(
                            normalizedCode,
                            normalizedEmail,
                            exception
                    );
        }
    }

    @Transactional(readOnly = true)
    public InstructorResult getById(
            UUID instructorId
    ) {
        return InstructorResult.from(
                findInstructor(instructorId)
        );
    }

    @Transactional(readOnly = true)
    public InstructorPageResult search(
            String search,
            String specialization,
            Boolean active,
            int page,
            int size
    ) {
        validatePagination(page, size);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("lastName"),
                        Sort.Order.asc("firstName"),
                        Sort.Order.asc("code")
                )
        );

        Page<Instructor> instructorPage =
                instructorRepository.search(
                        normalizeFilter(search),
                        normalizeFilter(specialization),
                        active,
                        pageRequest
                );

        List<InstructorResult> content =
                instructorPage.getContent()
                        .stream()
                        .map(InstructorResult::from)
                        .toList();

        return InstructorPageResult.from(
                instructorPage,
                content
        );
    }

    @Transactional
    public InstructorResult update(
            UUID instructorId,
            String firstName,
            String lastName,
            String email,
            String phone,
            String specialization,
            String bio
    ) {
        Instructor instructor =
                findInstructor(instructorId);

        String normalizedEmail =
                Instructor.normalizedEmail(email);

        if (normalizedEmail != null
                && instructorRepository
                .existsByEmailAndIdNot(
                        normalizedEmail,
                        instructorId
                )) {
            throw DuplicateInstructorException
                    .forEmail(normalizedEmail);
        }

        instructor.updateDetails(
                firstName,
                lastName,
                normalizedEmail,
                phone,
                specialization,
                bio,
                Instant.now(clock)
        );

        try {
            instructorRepository.flush();

            return InstructorResult.from(
                    instructor
            );

        } catch (DataIntegrityViolationException exception) {
            if (normalizedEmail != null) {
                throw DuplicateInstructorException
                        .forEmail(
                                normalizedEmail,
                                exception
                        );
            }

            throw exception;
        }
    }

    @Transactional
    public InstructorResult changeStatus(
            UUID instructorId,
            boolean active
    ) {
        Instructor instructor =
                findInstructor(instructorId);

        instructor.changeStatus(
                active,
                Instant.now(clock)
        );

        return InstructorResult.from(instructor);
    }

    private Instructor findInstructor(
            UUID instructorId
    ) {
        return instructorRepository
                .findById(instructorId)
                .orElseThrow(
                        () -> new InstructorNotFoundException(
                                instructorId
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