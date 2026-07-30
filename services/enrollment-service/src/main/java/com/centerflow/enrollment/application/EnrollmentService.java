package com.centerflow.enrollment.application;

import com.centerflow.enrollment.api.dto.CreateEnrollmentRequest;
import com.centerflow.enrollment.api.dto.EnrollmentResponse;
import com.centerflow.enrollment.api.dto.PageResponse;
import com.centerflow.enrollment.domain.Enrollment;
import com.centerflow.enrollment.domain.EnrollmentStatus;
import com.centerflow.enrollment.exception.EnrollmentConflictException;
import com.centerflow.enrollment.exception.EnrollmentNotFoundException;
import com.centerflow.enrollment.exception.InvalidPaginationException;
import com.centerflow.enrollment.number.EnrollmentNumberGenerator;
import com.centerflow.enrollment.repository.EnrollmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
public class EnrollmentService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<EnrollmentStatus>
            OPEN_ENROLLMENT_STATUSES = EnumSet.of(
            EnrollmentStatus.PENDING_PAYMENT,
            EnrollmentStatus.ACTIVE,
            EnrollmentStatus.SUSPENDED
    );

    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentNumberGenerator numberGenerator;

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            EnrollmentNumberGenerator numberGenerator
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.numberGenerator = numberGenerator;
    }

    @Transactional
    public EnrollmentResponse createEnrollment(
            CreateEnrollmentRequest request
    ) {
        validateNoOpenEnrollment(
                request.studentId(),
                request.batchId()
        );

        String enrollmentNumber =
                numberGenerator.nextNumber();

        Enrollment enrollment = Enrollment.create(
                enrollmentNumber,
                request.studentId(),
                request.batchId()
        );

        Enrollment savedEnrollment =
                enrollmentRepository.save(enrollment);

        return EnrollmentResponse.from(savedEnrollment);
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollment(
            UUID enrollmentId
    ) {
        return EnrollmentResponse.from(
                getRequiredEnrollment(enrollmentId)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<EnrollmentResponse>
    searchEnrollments(
            String enrollmentNumber,
            UUID studentId,
            UUID batchId,
            EnrollmentStatus status,
            int page,
            int size
    ) {
        validatePagination(page, size);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<Enrollment> enrollmentPage =
                enrollmentRepository.search(
                        normalizeSearchText(
                                enrollmentNumber
                        ),
                        studentId,
                        batchId,
                        status,
                        pageRequest
                );

        return PageResponse.from(
                enrollmentPage,
                EnrollmentResponse::from
        );
    }

    @Transactional
    public EnrollmentResponse activateEnrollment(
            UUID enrollmentId
    ) {
        Enrollment enrollment =
                enrollmentRepository
                        .findByIdForUpdate(enrollmentId)
                        .orElseThrow(
                                () ->
                                        new EnrollmentNotFoundException(
                                                enrollmentId
                                        )
                        );

        if (
                enrollment.getStatus()
                        == EnrollmentStatus.ACTIVE
        ) {
            return EnrollmentResponse.from(enrollment);
        }

        enrollment.activate();

        return EnrollmentResponse.from(enrollment);
    }

    @Transactional
    public EnrollmentResponse suspendEnrollment(
            UUID enrollmentId
    ) {
        Enrollment enrollment =
                getRequiredEnrollment(enrollmentId);

        enrollment.suspend();

        return EnrollmentResponse.from(enrollment);
    }

    @Transactional
    public EnrollmentResponse resumeEnrollment(
            UUID enrollmentId
    ) {
        Enrollment enrollment =
                getRequiredEnrollment(enrollmentId);

        enrollment.resume();

        return EnrollmentResponse.from(enrollment);
    }

    @Transactional
    public EnrollmentResponse completeEnrollment(
            UUID enrollmentId
    ) {
        Enrollment enrollment =
                getRequiredEnrollment(enrollmentId);

        enrollment.complete();

        return EnrollmentResponse.from(enrollment);
    }

    @Transactional
    public EnrollmentResponse cancelEnrollment(
            UUID enrollmentId
    ) {
        Enrollment enrollment =
                getRequiredEnrollment(enrollmentId);

        enrollment.cancel();

        return EnrollmentResponse.from(enrollment);
    }

    private Enrollment getRequiredEnrollment(
            UUID enrollmentId
    ) {
        return enrollmentRepository
                .findById(enrollmentId)
                .orElseThrow(
                        () -> new EnrollmentNotFoundException(
                                enrollmentId
                        )
                );
    }

    private void validateNoOpenEnrollment(
            UUID studentId,
            UUID batchId
    ) {
        boolean alreadyEnrolled =
                enrollmentRepository
                        .existsByStudentIdAndBatchIdAndStatusIn(
                                studentId,
                                batchId,
                                OPEN_ENROLLMENT_STATUSES
                        );

        if (alreadyEnrolled) {
            throw new EnrollmentConflictException(
                    "Student already has an open enrollment "
                            + "in this batch"
            );
        }
    }

    private void validatePagination(
            int page,
            int size
    ) {
        if (page < 0) {
            throw new InvalidPaginationException(
                    "Page index must be zero or greater"
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidPaginationException(
                    "Page size must be between 1 and "
                            + MAX_PAGE_SIZE
            );
        }
    }

    private String normalizeSearchText(
            String searchText
    ) {
        if (
                searchText == null
                        || searchText.isBlank()
        ) {
            return null;
        }

        return searchText.trim();
    }
}